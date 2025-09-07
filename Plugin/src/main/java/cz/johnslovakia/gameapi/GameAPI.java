package cz.johnslovakia.gameapi;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketListener;
import com.infernalsuite.aswm.loaders.mysql.MysqlLoader;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import cz.johnslovakia.gameapi.api.Schematic;
import cz.johnslovakia.gameapi.api.SlimeWorldLoader;
import cz.johnslovakia.gameapi.api.UserInterface;
import cz.johnslovakia.gameapi.api.VersionSupport;
import cz.johnslovakia.gameapi.datastorage.*;
import cz.johnslovakia.gameapi.game.GameManager;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.cosmetics.listeners.HatsCategoryListener;
import cz.johnslovakia.gameapi.game.cosmetics.listeners.KillEffectsCategoryListener;
import cz.johnslovakia.gameapi.game.cosmetics.listeners.KillSoundsCategoryListener;
import cz.johnslovakia.gameapi.game.kit.Kit;
import cz.johnslovakia.gameapi.game.kit.KitListener;
import cz.johnslovakia.gameapi.game.kit.KitManager;
import cz.johnslovakia.gameapi.game.map.MapManager;
import cz.johnslovakia.gameapi.game.perk.Perk;
import cz.johnslovakia.gameapi.game.perk.PerkManager;
import cz.johnslovakia.gameapi.guis.KitInventoryEditor;
import cz.johnslovakia.gameapi.guis.ProfileInventory;
import cz.johnslovakia.gameapi.levelSystem.LevelProgress;
import cz.johnslovakia.gameapi.serverManagement.DataManager;
import cz.johnslovakia.gameapi.serverManagement.gameData.GameDataManager;
import cz.johnslovakia.gameapi.levelSystem.LevelManager;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.quests.QuestManager;
import cz.johnslovakia.gameapi.users.resources.*;
import cz.johnslovakia.gameapi.guis.ViewPlayerInventory;
import cz.johnslovakia.gameapi.listeners.*;
import cz.johnslovakia.gameapi.messages.Language;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.*;
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
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
public class GameAPI{

    @Getter
    public static GameAPI instance;
    private Minigame minigame;

    private String version;
    private VersionSupport versionSupport;
    private UserInterface userInterface;
    private SlimeWorldLoader slimeWorldLoader;
    private Schematic schematicHandler;
    public boolean useDecentHolograms;

    private Chat vaultChat;
    private Permission vaultPerms;
    private Economy vaultEconomy;
    private ProtocolManager protocolManager;

    //TODO: přepsat celou tuto třídu
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
                            if (!(sender instanceof Player player)) {
                                return 0;
                            }
                            player.sendMessage(Component.text("Usage: /rate <1-5> or your feedback", NamedTextColor.YELLOW));
                            return 0;
                        })
                        .then(Commands.argument("rating", IntegerArgumentType.integer(1, 5))
                                .executes(context -> {
                                    CommandSender sender = context.getSource().getSender();
                                    if (!(sender instanceof Player player)) {
                                        return 0;
                                    }

                                    int rating = IntegerArgumentType.getInteger(context, "rating");

                                    SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
                                    if (connection == null) {
                                        return 0;
                                    }

                                    Optional<Row> result2 = connection.select()
                                            .from("TestServer")
                                            .where().isEqual("Nickname", player.getName())
                                            .obtainOne();

                                    player.sendMessage(Component.text("Thanks for your feedback! We truly appreciate it!", NamedTextColor.GREEN));
                                    if (result2.isEmpty()) {
                                        connection.insert()
                                                .into("TestServer", "Nickname", "Minigame", "Stars", "Version")
                                                .values(player.getName(), Minigame.getInstance().getName(), rating, Minigame.getInstance().getPlugin().getDescription().getVersion())
                                                .execute();
                                        if (rating < 5){
                                            player.sendMessage(Component.text("We'd appreciate it if you shared the reason for your rating: /rate <feedback>", TextColor.color(199, 15, 209)));
                                            player.sendMessage("§eWe'll keep improving the plugin to make it as great as possible.");
                                        }
                                    }else{
                                        connection.update()
                                                .table("TestServer")
                                                .set("Stars", rating)
                                                .execute();
                                        if (result2.get().get("Feedback") == null && rating < 5){
                                            player.sendMessage(Component.text("We'd appreciate it if you shared the reason for your rating: /rate <feedback>", TextColor.color(170, 7, 179)));
                                            player.sendMessage("§eWe'll keep improving the plugin to make it as great as possible.");
                                        }
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.argument("feedback", StringArgumentType.greedyString())
                                .executes(context -> {
                                    CommandSender sender = context.getSource().getSender();
                                    if (!(sender instanceof Player player)) {
                                        return 0;
                                    }

                                    String feedback = context.getArgument("feedback", String.class);

                                    SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
                                    if (connection == null) {
                                        return 0;
                                    }

                                    Optional<Row> result2 = connection.select()
                                            .from("TestServer")
                                            .where().isEqual("Nickname", player.getName())
                                            .obtainOne();

                                    if (result2.isEmpty()) {
                                        connection.insert()
                                                .into("TestServer", "Nickname", "Minigame", "Feedback", "Version")
                                                .values(player.getName(), Minigame.getInstance().getName(), feedback, Minigame.getInstance().getPlugin().getDescription().getVersion())
                                                .execute();
                                    }else{
                                        connection.update()
                                                .table("TestServer")
                                                .set("Feedback", feedback)
                                                .execute();
                                    }
                                    player.sendMessage(Component.text("Thanks for your feedback! We truly appreciate it!", NamedTextColor.GREEN));
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

        if (plugin.getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            useDecentHolograms = true;
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
            final Class<?> clazz = Class.forName("cz.johnslovakia.gameapi.nms." + version + ".NMSHandler");
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

            if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null) {
                final Class<?> clazz3 = Class.forName("cz.johnslovakia.gameapi.nms." + version + ".SchematicHandler");
                if (Schematic.class.isAssignableFrom(clazz3)) {
                    this.schematicHandler = (Schematic) clazz3.getConstructor().newInstance();
                }
            }
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
        GameManager.getGames().clear();
        PlayerManager.getPlayerMap().clear();
        KitManager.getKitManagers().clear();
    }

    public void checkMessages(File gFile, File mainFile){
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(gFile), StandardCharsets.UTF_8))) {
            if (!mainFile.exists() || mainFile.length() == 0) return;

            try (RandomAccessFile raf = new RandomAccessFile(mainFile, "rw")) {
                raf.seek(raf.length() - 1);
                byte lastByte = raf.readByte();
                if (lastByte != '\n') {
                    raf.seek(raf.length());
                    raf.write('\n');
                }
            }


            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

                int colonIndex = line.indexOf(':');
                if (colonIndex == -1) continue;

                String key = line.substring(0, colonIndex).trim();
                if (containsKey(mainFile, key)) continue;


                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mainFile, true), StandardCharsets.UTF_8))) {
                    writer.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void registerMinigame(Minigame minigame){
        this.minigame = minigame;

        Plugin plugin = minigame.getPlugin();
        onEnable(minigame);


        boolean somethingwrong = false;

        File pluginLanguagesFolder = new File(minigame.getPlugin().getDataFolder(), "languages");

        if (!pluginLanguagesFolder.exists()) {
            pluginLanguagesFolder.mkdirs();
        }

        try {
            Bukkit.getLogger().log(Level.INFO, "Processing language files...");

            for (InputStreamWithName is : minigame.getLanguageFiles()) {
                long startTime = System.currentTimeMillis();

                String name = is.getFileName();

                File mainFile = new File(pluginLanguagesFolder, name);
                boolean exists = mainFile.exists();
                if (!exists)
                    minigame.getPlugin().saveResource("languages/" + name, false);

                File gFile = File.createTempFile("gFile", ".yml");
                InputStream in = GameAPI.class.getClassLoader().getResourceAsStream("gLanguages/" + name);
                if (in != null) FileUtils.copyInputStreamToFile(in, gFile);


                checkMessages(gFile, mainFile);

                if (exists) {
                    File check = File.createTempFile("cFile", ".yml");
                    InputStream in2 = minigame.getPlugin().getResource("languages/" + name);
                    if (in2 != null) FileUtils.copyInputStreamToFile(in2, check);

                    checkMessages(check, mainFile);
                }


                loadMessagesFromFile(mainFile);
                Bukkit.getLogger().log(Level.INFO, "Processing of language file " + name + " completed (" + (System.currentTimeMillis() - startTime) + "ms)");
            }
        } catch (IOException e) {
            Logger.log("Something went wrong when retrieving messages! The following message is for Developers: ", Logger.LogType.ERROR);
            e.printStackTrace();
            somethingwrong = true;
        }

        try{
            //TODO: vyřešit název
            for (int i = 1; i <= minigame.getSettings().getGamesPerServer(); i++){
                minigame.setupGame(minigame.getName() + "-" + i);
            }
            Logger.log("Games successfully loaded!", Logger.LogType.INFO);
        }catch (Exception e){
            Logger.log("Something went wrong when loading the maps! The following message is for Developers: ", Logger.LogType.ERROR);
            e.printStackTrace();
            somethingwrong = true;
        }


        if (Minigame.getInstance().getDatabase() == null){
            Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
            return;
        }


        Resource experiencePoints = new Resource("ExperiencePoints", ChatColor.YELLOW, 1, true, true);
        experiencePoints.setDisplayName("Experience Points");
        Resource cosmeticTokens = new Resource("CosmeticTokens", ChatColor.DARK_GREEN, true, true);
        cosmeticTokens.setDisplayName("Cosmetic Tokens");

        ResourcesManager.addResource(experiencePoints, cosmeticTokens);


        minigame.setupPlayerScores();


        MinigameTable minigameTable = minigame.getMinigameTable();
        PlayerTable playerTable = new PlayerTable();

        SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();


        JSConfigs.createTable(connection);
        //TODO: možnost nepoužívat levelManager
        LevelManager levelManager;
        if (minigame.getSettings().isUseLevelSystem()) {
            levelManager = LevelManager.loadOrCreateLevelManager();
            minigame.setLevelManager(levelManager);
        } else {
            levelManager = null;
        }


        try{
            UnclaimedRewardsTable.createTable();
            minigameTable.createTable();
            playerTable.createTable();

            minigameTable.createNewColumn(Type.VARCHAR32, "LastDailyWinReward");


            minigame.setupOther();


            if (levelManager != null){
                playerTable.createNewColumn(Type.INT, "Level", "1");
                playerTable.createNewColumn(Type.INT, "DailyXP");
                playerTable.createNewColumn(Type.VARCHAR128, "DailyRewards_reset");
                playerTable.createNewColumn(Type.INT, "DailyRewards_claims");
            }
        }catch (Exception e){
            Logger.log("Something went wrong when setting up the other necessities for the minigame! The following message is for Developers: ", Logger.LogType.ERROR);
            e.printStackTrace();
            somethingwrong = true;
        }


        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIExpansion(minigame).register();
        }


        for (Resource resource : ResourcesManager.getResources()){
            if (resource.isAutomatically()){
                if (!resource.isForAllMinigames()){
                    minigameTable.createNewColumn(Type.INT , resource.getName());
                }else{
                    playerTable.createNewColumn(Type.INT, resource.getName());
                }
            }
        }

        if (levelManager != null){
            experiencePoints.observe((gamePlayer, amount, type) -> {
                if (type == ResourceChangeType.DEPOSIT) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, task -> minigame.getLevelManager().isLevelUp(gamePlayer));

                    if (gamePlayer.getPlayerData().getDailyMeterTier() != null){
                        if (gamePlayer.getPlayerData().getDailyMeterTier().tier() <= minigame.getLevelManager().getDailyMeter().getMaxTier()) {
                            gamePlayer.getPlayerData().addDailyXP(amount);
                        }
                    }


                    Player player = gamePlayer.getOnlinePlayer();
                    LevelProgress levelProgress = levelManager.getLevelProgress(gamePlayer);
                    float xpProgress = (float) levelProgress.xpOnCurrentLevel() / levelProgress.levelRange().neededXP();

                    if (!gamePlayer.getGame().getState().equals(GameState.INGAME)) {
                        Bukkit.getScheduler().runTask(minigame.getPlugin(), task -> {
                            player.setExp(Math.min(xpProgress, 1.0f));
                            player.setLevel(levelProgress.level());
                        });
                    }
                }
            });
        }


        if (somethingwrong) {
            Logger.log("I can't register the minigame due to previous problems!", Logger.LogType.ERROR);
        }else{
            Logger.log("Minigame successfully registered!", Logger.LogType.INFO);
        }


        if (minigame.getCosmeticsManager() != null){
            Bukkit.getPluginManager().registerEvents(minigame.getCosmeticsManager(), plugin);
        }
    }

    private boolean containsKey(File file, String key) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(key + ":")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void loadMessagesFromFile(File file) {
        String nr = "\n";

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                int colonIndex = line.indexOf(':');
                if (colonIndex != -1) {
                    String key = line.substring(0, colonIndex).trim();
                    String value = line.substring(colonIndex + 1).trim();

                    if (file.getName().contains("scoreboard")){
                        continue;
                    }
                    String languageName = file.getName().replace(".yml", "");
                    Language language = Language.getLanguage(languageName);
                    if (language == null){
                        language = Language.addLanguage(new Language(languageName));
                    }

                    String message = value.replace("\"", "");
                    MessageManager.addMessage(key, language, message);
                }
            }
        } catch (IOException e) {
            Logger.log("An error occurred while loading the file: " + file.getName() + ": " + e.getMessage(), Logger.LogType.WARNING);
        }
    }

    public boolean useDecentHolograms() {
        return useDecentHolograms;
    }

    public File getMinigameDataFolder(){
        return getMinigame().getPlugin().getDataFolder();
    }
}
