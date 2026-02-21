package cz.johnslovakia.gameapi;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.loaders.mysql.MysqlLoader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import cz.johnslovakia.gameapi.database.*;
import cz.johnslovakia.gameapi.listeners.cosmetics.HatsCategoryListener;
import cz.johnslovakia.gameapi.listeners.cosmetics.KillEffectsCategoryListener;
import cz.johnslovakia.gameapi.listeners.cosmetics.KillSoundsCategoryListener;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.modules.dailyRewardTrack.DailyRewardTrackModule;
import cz.johnslovakia.gameapi.modules.game.GameService;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.killMessage.KillMessageModule;
import cz.johnslovakia.gameapi.modules.kits.Kit;
import cz.johnslovakia.gameapi.modules.kits.KitListener;
import cz.johnslovakia.gameapi.modules.kits.KitManager;
import cz.johnslovakia.gameapi.guis.KitInventoryEditor;
import cz.johnslovakia.gameapi.guis.ProfileInventory;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.modules.levels.PlayerLevelData;
import cz.johnslovakia.gameapi.modules.messages.FileGroup;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.perks.Perk;
import cz.johnslovakia.gameapi.modules.perks.PerkManager;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourceChangeType;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.modules.serverManagement.DataManager;
import cz.johnslovakia.gameapi.rewards.unclaimed.UnclaimedRewardsModule;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.*;
import cz.johnslovakia.gameapi.worldManagement.SlimeWorldLoader;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.guis.ViewPlayerInventory;
import cz.johnslovakia.gameapi.listeners.*;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.chatHead.ChatHeadAPI;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import lombok.Getter;
import me.zort.containr.Containr;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.Row;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.*;

@Getter
public class GameAPI{

    @Getter
    public static GameAPI instance;
    private Minigame minigame;

    private String version;
    private SlimeWorldLoader slimeWorldLoader;

    private Chat vaultChat;
    private Permission vaultPerms;
    private Economy vaultEconomy;
    private ProtocolManager protocolManager;

    //TODO: přepsat ?
    public GameAPI(Minigame minigame) {
        instance = this;
        this.minigame = minigame;
        registerMinigame(minigame);
    }

    public void onEnable(Minigame minigame) {
        JavaPlugin plugin = minigame.getPlugin();
        Containr.init(plugin);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");

        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> saveInventory = Commands.literal("saveinventory")
                    .executes(context -> {
                        CommandSender source = context.getSource().getSender();
                        if (source instanceof Player player){
                            GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

                            if (gamePlayer.getMetadata().get("set_kit_inventory.kit") == null){
                                return 0;
                            }

                            Inventory kitInventory = (Inventory) gamePlayer.getMetadata().get("set_kit_inventory.inventory");
                            Kit kit = (Kit) gamePlayer.getMetadata().get("set_kit_inventory.kit");

                            KitInventoryEditor.save(gamePlayer, kit, kitInventory, (boolean) gamePlayer.getMetadata().get("set_kit_inventory.autoArmor"));
                        }
                        return 1;
                    })
                    .build();
            commands.registrar().register(saveInventory);

            LiteralCommandNode<CommandSourceStack> profile = Commands.literal("profile")
                    .executes(context -> {
                        CommandSender source = context.getSource().getSender();
                        if (source instanceof Player player){
                            GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
                            ProfileInventory.openGUI(gamePlayer);
                        }
                        return 1;
                    })
                    .build();
            commands.registrar().register(profile);

            if (minigame.isTestServer()) {
                LiteralCommandNode<CommandSourceStack> rateCommand = Commands.literal("rate")
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            if (!(sender instanceof Player player)) return 0;

                            player.sendMessage(Component.text("Usage: /rate <1-5> or your feedback", NamedTextColor.YELLOW));
                            return 0;
                        })
                        .then(Commands.argument("rating", IntegerArgumentType.integer(1, 5))
                                .executes(context -> {
                                    CommandSender sender = context.getSource().getSender();
                                    if (!(sender instanceof Player player)) return 0;

                                    int rating = IntegerArgumentType.getInteger(context, "rating");

                                    try (SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection()) {
                                        if (connection == null) return 0;

                                        Optional<Row> result = connection.select()
                                                .from("TestServer")
                                                .where().isEqual("Nickname", player.getName())
                                                .obtainOne();

                                        player.sendMessage(Component.text(
                                                "Thanks for your feedback! We truly appreciate it!",
                                                NamedTextColor.GREEN
                                        ));

                                        if (result.isEmpty()) {
                                            connection.insert()
                                                    .into("TestServer", "Nickname", "Minigame", "Stars", "Version")
                                                    .values(
                                                            player.getName(),
                                                            Minigame.getInstance().getName(),
                                                            rating,
                                                            Minigame.getInstance().getPlugin().getDescription().getVersion()
                                                    )
                                                    .execute();

                                            if (rating < 5) {
                                                player.sendMessage(Component.text(
                                                        "We'd appreciate it if you shared the reason for your rating: /rate <feedback>",
                                                        TextColor.color(199, 15, 209)
                                                ));
                                                player.sendMessage("§eWe'll keep improving the plugin to make it as great as possible.");
                                            }
                                        } else {
                                            connection.update()
                                                    .table("TestServer")
                                                    .set("Stars", rating)
                                                    .where().isEqual("Nickname", player.getName())
                                                    .execute();

                                            if (result.get().get("Feedback") == null && rating < 5) {
                                                player.sendMessage(Component.text(
                                                        "We'd appreciate it if you shared the reason for your rating: /rate <feedback>",
                                                        TextColor.color(170, 7, 179)
                                                ));
                                                player.sendMessage("§eWe'll keep improving the plugin to make it as great as possible.");
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    return 1;
                                })
                        )
                        .then(Commands.argument("feedback", StringArgumentType.greedyString())
                                .executes(context -> {
                                    CommandSender sender = context.getSource().getSender();
                                    if (!(sender instanceof Player player)) return 0;

                                    String feedback = context.getArgument("feedback", String.class);

                                    try (SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection()) {
                                        if (connection == null) return 0;

                                        Optional<Row> result = connection.select()
                                                .from("TestServer")
                                                .where().isEqual("Nickname", player.getName())
                                                .obtainOne();

                                        if (result.isEmpty()) {
                                            connection.insert()
                                                    .into("TestServer", "Nickname", "Minigame", "Feedback", "Version")
                                                    .values(
                                                            player.getName(),
                                                            Minigame.getInstance().getName(),
                                                            feedback,
                                                            Minigame.getInstance().getPlugin().getDescription().getVersion()
                                                    )
                                                    .execute();
                                        } else {
                                            connection.update()
                                                    .table("TestServer")
                                                    .set("Feedback", feedback)
                                                    .where().isEqual("Nickname", player.getName())
                                                    .execute();
                                        }

                                        player.sendMessage(Component.text(
                                                "Thanks for your feedback! We truly appreciate it!",
                                                NamedTextColor.GREEN
                                        ));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    return 1;
                                })
                        )
                        .build();

                commands.registrar().register(rateCommand);
            }
        });


        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Chat> rsp = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp != null) {
                vaultChat = rsp.getProvider();
            }
            RegisteredServiceProvider<Permission> rsp2 = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
            if (rsp2 != null) {
                vaultPerms = rsp2.getProvider();
            }
            RegisteredServiceProvider<Economy> rsp3 = plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp3 != null) {
                vaultEconomy = rsp3.getProvider();
            }
        }


        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new AreaListener(), plugin);
        pm.registerEvents(new ChatListener(), plugin);
        pm.registerEvents(new JoinQuitListener(), plugin);
        pm.registerEvents(new MapSettingsListener(), plugin);
        pm.registerEvents(new PlayerDeathListener(), plugin);
        pm.registerEvents(new PVPListener(), plugin);
        pm.registerEvents(new RespawnListener(), plugin);
        pm.registerEvents(new WorldListener(), plugin);
        pm.registerEvents(new AntiAFK(), plugin);
        pm.registerEvents(new ViewPlayerInventory(), plugin);
        pm.registerEvents(new HologramListener(), plugin);
        pm.registerEvents(new ItemPickupListener(), plugin);
        pm.registerEvents(new InteractListener(), plugin);
        pm.registerEvents(new KitListener(), plugin);
        pm.registerEvents(new HatsCategoryListener(), plugin);
        pm.registerEvents(new KillEffectsCategoryListener(), plugin);
        pm.registerEvents(new KillSoundsCategoryListener(), plugin);
        pm.registerEvents(new AbilityItemListener(), plugin);

        new ItemUtils(plugin);

        Bukkit.getPluginManager().registerEvents(new KitInventoryEditor(), plugin);


        protocolManager = ProtocolLibrary.getProtocolManager();

        //PacketListener listener = new ProtocolTagChanger(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.BOSS);
        //protocolManager.addPacketListener(listener);


        ChatHeadAPI.initialize(plugin);


        //VERSION SUPPORT //TODO: opravit pro další verze
        String packageName = plugin.getServer().getClass().getPackage().getName();
        String version = "v1_21_R1";//packageName.substring(packageName.lastIndexOf('.') + 1); //vrací craftbukkit
        this.version = version;

        try {
            /*final Class<?> clazz = Class.forName("cz.johnslovakia.gameapi.nms." + version + ".NMSHandler");
            if (VersionSupport.class.isAssignableFrom(clazz)) {
                this.versionSupport = (VersionSupport) clazz.getConstructor().newInstance();
            }

            final Class<?> clazz2 = Class.forName("cz.johnslovakia.gameapi.nms." + version + ".UserInterfaceHandler");
            if (UserInterface.class.isAssignableFrom(clazz2)) {
                this.userInterface = (UserInterface) clazz2.getConstructor().newInstance();
            }
            /*if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin"/*"SlimeWorldManager"*) != null) {
                final Class<?> clazz3 = Class.forName("cz.johnslovakia.gameapi.nms." + version + ".SlimeWorldLoaderHandler");
                if (SlimeWorldLoader.class.isAssignableFrom(clazz3)) {
                    //this.slimeWorldLoader = (SlimeWorldLoader) clazz3.getConstructor().newInstance();
                    this.slimeWorldLoader = (SlimeWorldLoader) clazz3.getConstructor(MysqlLoader.class).newInstance();
                }
            }*/

            /*if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null) {
                final Class<?> clazz3 = Class.forName("cz.johnslovakia.gameapi.nms." + version + ".SchematicHandler");
                if (Schematic.class.isAssignableFrom(clazz3)) {
                    this.schematicHandler = (Schematic) clazz3.getConstructor().newInstance();
                }
            }*/
        } catch (final Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe("Could not find support for this Spigot version.");
            return;
        }
        plugin.getLogger().info("Loading support for " + version);

        if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin"/*"SlimeWorldManager"*/) != null) {
            final Class<?> clazz3;
            try {
                clazz3 = Class.forName("cz.johnslovakia.gameapi.nms." + version + ".SlimeWorldLoaderHandler");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            if (SlimeWorldLoader.class.isAssignableFrom(clazz3)) {
                //this.slimeWorldLoader = (SlimeWorldLoader) clazz3.getConstructor().newInstance();
                try {
                    this.slimeWorldLoader = (SlimeWorldLoader) clazz3.getConstructor(MysqlLoader.class).newInstance(minigame.getDatabase().getAswmLoader());
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        }



        File worldContainer = Bukkit.getWorldContainer();
        File[] worldsToDelete = worldContainer.listFiles(file ->
                file.isDirectory() && file.getName().matches(".+_[a-zA-Z0-9]{6}")
        );

        if (worldsToDelete != null) {
            for (File worldFolder : worldsToDelete) {
                if (worldFolder.getName().equalsIgnoreCase("world") || worldFolder.getName().equalsIgnoreCase("nether")) continue;
                Logger.log("Deleting world folder: " + worldFolder.getName(), Logger.LogType.INFO);
                if (Bukkit.getWorld(worldFolder.getName()) != null){
                    Bukkit.unloadWorld(worldFolder.getName(), false);
                }
                FileManager.deleteFile(worldFolder);
            }
        }
    }

    public void onDisable(Plugin plugin) {
        if (DataManager.getInstance() != null && DataManager.getInstance().getServerDataRedis() != null) DataManager.getInstance().getServerDataRedis().close();
        if (getMinigame().getDatabase() != null) getMinigame().getDatabase().getConnection().disconnect();

        if (protocolManager != null) {
            protocolManager.removePacketListeners(plugin);
        }

        Logger.closeAllWriters();
        HandlerList.unregisterAll(plugin);

        PerkManager perkManager = Minigame.getInstance().getPerkManager();
        if (perkManager != null){
            for (Perk perk : perkManager.getPerks()){
                HandlerList.unregisterAll(perk);
            }
        }
        PlayerIdentityRegistry.map.clear();
        KitManager.getKitManagers().clear();
        ModuleManager.getInstance().destroyAll();
    }


    public void registerMinigame(Minigame minigame){
        this.minigame = minigame;

        JavaPlugin plugin = minigame.getPlugin();
        
        onEnable(minigame);
        new Shared(plugin, minigame.getDatabase());

        boolean somethingwrong = false;
        //TODO: something went wrong messages

        ModuleManager moduleManager = minigame.getModuleManager();
        moduleManager.registerModule(new GameService());
        moduleManager.registerModule(new KillMessageModule());
        moduleManager.registerModule(new UnclaimedRewardsModule());
        ResourcesModule resourcesModule = moduleManager.registerModule(new ResourcesModule());
        

        List<FileGroup> fileGroups = new ArrayList<>();
        for (Map.Entry<String, InputStreamWithName> entry : minigame.getLanguageFiles().entrySet()) {
           List<InputStream> streams = new ArrayList<>();
            streams.add(entry.getValue().inputStream());

            InputStream in = GameAPI.class.getClassLoader().getResourceAsStream("gLanguages/" + entry.getValue().fileName());
            if (in != null) streams.add(in);

            fileGroups.add(new FileGroup(entry.getKey(), streams));
        }
        moduleManager.registerModule(new MessageModule(plugin, fileGroups));



        try{
            //TODO: vyřešit název
            boolean bungeecord = DataManager.getInstance().getServerDataMySQL() != null || DataManager.getInstance().getServerDataRedis() != null;
            int gamesPerServer = minigame.getSettings().getGamesPerServer();
            for (int i = 0; i < gamesPerServer; i++) {
                if (bungeecord) {
                    minigame.setupGame((minigame.getSettings().getServerName() != "" ? minigame.getSettings().getServerName() : minigame.getName() + "-1")
                            + (gamesPerServer > 1 ? StringUtils.getLetter(i) : ""));
                } else {
                    minigame.setupGame(minigame.getName() + "-" + (i + 1));
                }
            }
            Logger.log("Games successfully loaded!", Logger.LogType.INFO);
        }catch (Exception e){
            Logger.log("Something went wrong when loading games! The following message is for Developers: ", Logger.LogType.ERROR);
            e.printStackTrace();
            somethingwrong = true;
        }


        if (Minigame.getInstance().getDatabase() == null){
            Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
            return;
        }


        Resource experiencePoints = Resource.builder("ExperiencePoints")
                .displayName("Experience Points")
                .color(ChatColor.YELLOW)
                .rank(1)
                .batched("gameapi_playertable")
                .build();
        Resource cosmeticTokens = Resource.builder("CosmeticTokens")
                .displayName("Cosmetic Tokens")
                .color(ChatColor.DARK_GREEN)
                .batched("gameapi_playertable")
                .build();

        FileConfiguration config = minigame.getPlugin().getConfig();
        boolean vault = false;
        RegisteredServiceProvider<Economy> rsp = null;
        if (Bukkit.getPluginManager().getPlugin("Vault") != null && config.getBoolean("useVault")) {
            vault = true;
            rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        }

        Resource.Builder coins = Resource.builder("Coins")
                .color(ChatColor.GOLD)
                .rank(2)
                .applicableBonus(true);
        if (vault && rsp != null){
            Economy vaultEconomy = rsp.getProvider();
            coins.vault(vaultEconomy);
        }else{
            coins.batched("gameapi_playertable");
        }

        resourcesModule.registerResource(coins.build(), experiencePoints, cosmeticTokens);


        minigame.setupPlayerScores();


        MinigameTable minigameTable = minigame.getMinigameTable();
        PlayerTable playerTable = new PlayerTable();

        JSConfigs.createTable();
        LevelModule levelModule;
        if (minigame.getSettings().isUseLevelSystem()) {
            levelModule = LevelModule.loadOrCreateLevelModule();
            moduleManager.registerModule(levelModule);
        } else {
            levelModule = null;
        }
        DailyRewardTrackModule dailyRewardTrackModule;
        if (minigame.getSettings().isUseDailyRewardTrack()) {
            dailyRewardTrackModule = DailyRewardTrackModule.loadOrCreateDailyRewardTrackModule();
            moduleManager.registerModule(dailyRewardTrackModule);
        } else {
            dailyRewardTrackModule = null;
        }


        try{
            UnclaimedRewardsTable.createTable();
            minigameTable.createTable();
            playerTable.createTable();

            minigameTable.createNewColumn(Type.VARCHAR32, "LastDailyWinReward");
            moduleManager.registerModule(new CosmeticsModule());


            minigame.setupOther();


            if (levelModule != null && dailyRewardTrackModule != null){
                //playerTable.createNewColumn(Type.INT, "Level", "1");
                playerTable.createNewColumn(Type.INT, "DailyXP");
                playerTable.createNewColumn(Type.VARCHAR128, "DailyRewards_reset");
                playerTable.createNewColumn(Type.INT, "DailyRewards_claims");
            }
        }catch (Exception e){
            Logger.log("Something went wrong when setting up the other necessities for the minigame! The following message is for Developers: ", Logger.LogType.ERROR);
            e.printStackTrace();
            somethingwrong = true;
        }

        try{
            DatabaseMigrationHelper.ensureNicknameUnique(PlayerTable.TABLE_NAME);
            DatabaseMigrationHelper.ensureNicknameUnique(minigameTable.getTableName());
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIExpansion(minigame).register();
        }


        for (Resource resource : resourcesModule.getResources()){
            if (resource.getResourceInterface() != null) resource.getResourceInterface().onEnable();
        }

        if (levelModule != null || dailyRewardTrackModule != null){
            experiencePoints.observe((gamePlayer, amount, type) -> {
                if (type == ResourceChangeType.DEPOSIT) {
                    if (levelModule != null) {
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, task -> levelModule.checkLevelUp(gamePlayer));

                        if (gamePlayer.getOfflinePlayer().isOnline()) {
                            Player player = gamePlayer.getOnlinePlayer();
                            PlayerLevelData levelProgress = levelModule.getPlayerData(gamePlayer);
                            if (levelProgress != null) {
                                levelProgress.calculate().thenRun(() -> {
                                    float xpProgress = (float) levelProgress.getXpOnCurrentLevel() / levelProgress.getXpToNextLevel();

                                    if (!((GamePlayer) gamePlayer).getGame().getState().equals(GameState.INGAME)) {
                                        Bukkit.getScheduler().runTask(minigame.getPlugin(), task -> {
                                            player.setExp(Math.min(xpProgress, 1.0f));
                                            player.setLevel(levelProgress.getLevel());
                                        });
                                    }
                                });
                            }
                        }
                    }


                    if (dailyRewardTrackModule != null && levelModule != null) {
                        if (dailyRewardTrackModule.getPlayerCurrentTier(gamePlayer) != null) {
                            if (dailyRewardTrackModule.getPlayerCurrentTier(gamePlayer).tier() <= dailyRewardTrackModule.getMaxTier()) {
                                levelModule.addDailyXP(gamePlayer, amount);
                            }
                        }
                    }
                }
            });
        }


        if (somethingwrong) {
            Logger.log("I can't register the minigame due to previous problems!", Logger.LogType.ERROR);
        }else{
            Logger.log("Minigame successfully registered!", Logger.LogType.INFO);
        }
    }

    public File getMinigameDataFolder(){
        return getMinigame().getPlugin().getDataFolder();
    }
}
