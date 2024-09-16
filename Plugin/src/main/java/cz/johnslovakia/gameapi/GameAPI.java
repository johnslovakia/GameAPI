package cz.johnslovakia.gameapi;

import cz.johnslovakia.gameapi.api.Schematic;
import cz.johnslovakia.gameapi.api.SlimeWorldLoader;
import cz.johnslovakia.gameapi.api.UserInterface;
import cz.johnslovakia.gameapi.api.VersionSupport;
import cz.johnslovakia.gameapi.datastorage.MinigameTable;
import cz.johnslovakia.gameapi.datastorage.PlayerTable;
import cz.johnslovakia.gameapi.datastorage.Type;
import cz.johnslovakia.gameapi.economy.EconomyInterface;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsManager;
import cz.johnslovakia.gameapi.game.kit.KitManager;
import cz.johnslovakia.gameapi.game.perk.PerkManager;
import cz.johnslovakia.gameapi.listeners.*;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.stats.StatsManager;
import cz.johnslovakia.gameapi.utils.BukkitSerialization;
import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import lombok.Setter;
import me.zort.containr.Containr;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.Row;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class GameAPI extends JavaPlugin {

    @Getter
    private static GameAPI instance;
    @Getter
    private Minigame minigame;

    @Getter @Setter
    private KitManager kitManager;
    @Getter @Setter
    private CosmeticsManager cosmeticsManager;
    @Getter @Setter
    private PerkManager perkManager;
    @Getter @Setter
    private StatsManager statsManager;


    @Getter
    private String version;
    @Getter
    private VersionSupport versionSupport;
    @Getter
    private UserInterface userInterface;
    @Getter
    private SlimeWorldLoader slimeWorldLoader;
    @Getter
    private Schematic schematicHandler;
    @Getter
    public static boolean useDecentHolograms;

    @Getter
    private Chat vaultChat;
    @Getter
    private Permission vaultPerms;
    @Getter
    private Economy vaultEconomy;

    @Override
    public void onEnable() {
        instance = this;

        Containr.init(this);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");


        if (getServer().getPluginManager().getPlugin("Vault") != null) {

            RegisteredServiceProvider<Chat> rsp = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp != null) {
                vaultChat = rsp.getProvider();
            }
            RegisteredServiceProvider<Permission> rsp2 = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
            if (rsp2 != null) {
                vaultPerms = rsp2.getProvider();
            }
            RegisteredServiceProvider<Economy> rsp3 = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp3 != null) {
                vaultEconomy = rsp3.getProvider();
            }
        }

        if (getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            useDecentHolograms = true;
        }



        //VERSION SUPPORT
        String packageName = this.getServer().getClass().getPackage().getName();
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);
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
            if (Bukkit.getPluginManager().getPlugin("SlimeWorldManager") != null) {
                final Class<?> clazz3 = Class.forName("cz.johnslovakia.gameapi.nms." + version + ".SlimeWorldLoaderHandler");
                if (SlimeWorldLoader.class.isAssignableFrom(clazz3)) {
                    this.slimeWorldLoader = (SlimeWorldLoader) clazz3.getConstructor().newInstance();
                }
            }

            if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null) {
                final Class<?> clazz3 = Class.forName("cz.johnslovakia.gameapi.nms." + version + ".SchematicHandler");
                if (Schematic.class.isAssignableFrom(clazz3)) {
                    this.schematicHandler = (Schematic) clazz3.getConstructor().newInstance();
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
            this.getLogger().severe("Could not find support for this Spigot version.");
            this.setEnabled(false);
            return;
        }
        this.getLogger().info("Loading support for " + version);


        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new AreaListener(), this);
        pm.registerEvents(new ChatListener(), this);
        pm.registerEvents(new JoinQuitListener(), this);
        pm.registerEvents(new MapSettingsListener(), this);
        pm.registerEvents(new PlayerDeathListener(), this);
        pm.registerEvents(new RespawnListener(), this);
        pm.registerEvents(new WorldListener(), this);
    }

    @Override
    public void onDisable() {

    }


    public void registerMinigame(Minigame minigame){

        boolean somethingwrong = false;
        try{
            minigame.setupMaps();
            Logger.log("Maps successfully loaded!", Logger.LogType.INFO);
        }catch (Exception e){
            Logger.log("Something went wrong when loading the maps! The following message is for Developers: " + e.getCause().getMessage(), Logger.LogType.ERROR);
            somethingwrong = true;
        }
        minigame.setupPlayerScores();
        try{
            minigame.setupOther();
        }catch (Exception e){
            Logger.log("Something went wrong when setting up the other necessities for the minigame! The following message is for Developers: " + e.getCause().getMessage(), Logger.LogType.ERROR);
            somethingwrong = true;
        }

        MinigameTable minigameTable = minigame.getMinigameTable();
        PlayerTable playerTable = new PlayerTable();

        for (cz.johnslovakia.gameapi.economy.Economy economy : minigame.getEconomies()){
            if (economy.isAutomatically()){
                SQLDatabaseConnection connection = GameAPI.getInstance().getMinigame().getDatabase();
                if (!economy.isForAllMinigames()){

                    minigameTable.addRow(Type.VARCHAR128 ,economy.getName());

                    economy.setEconomyInterface(new EconomyInterface() {
                        @Override
                        public void deposit(GamePlayer gamePlayer, int amount) {
                            Optional<Row> result = connection.select()
                                    .from(minigameTable.getTableName())
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .obtainOne();

                            connection.update()
                                    .table(minigameTable.getTableName())
                                    .set(economy.getName(), result.get().getInt(economy.getName()) + amount)
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .execute();
                        }

                        @Override
                        public void withdraw(GamePlayer gamePlayer, int amount) {
                            Optional<Row> result = connection.select()
                                    .from(minigameTable.getTableName())
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .obtainOne();

                            connection.update()
                                    .table(minigameTable.getTableName())
                                    .set(economy.getName(), result.get().getInt(economy.getName()) - amount)
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .execute();
                        }

                        @Override
                        public void setBalance(GamePlayer gamePlayer, int balance) {
                            connection.update()
                                    .table(minigameTable.getTableName())
                                    .set(economy.getName(), balance)
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .execute();
                        }

                        @Override
                        public int getBalance(GamePlayer gamePlayer) {
                            Optional<Row> result = connection.select()
                                    .from(minigameTable.getTableName())
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .obtainOne();
                            return result.get().getInt(economy.getName());
                        }
                    });
                }else{
                    playerTable.addRow(Type.INT, economy.getName());

                    economy.setEconomyInterface(new EconomyInterface() {
                        @Override
                        public void deposit(GamePlayer gamePlayer, int amount) {
                            Optional<Row> result = connection.select()
                                    .from(PlayerTable.TABLE_NAME)
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .obtainOne();

                            connection.update()
                                    .table(PlayerTable.TABLE_NAME)
                                    .set(economy.getName(), result.get().getInt(economy.getName()) + amount)
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .execute();
                        }

                        @Override
                        public void withdraw(GamePlayer gamePlayer, int amount) {
                            Optional<Row> result = connection.select()
                                    .from(PlayerTable.TABLE_NAME)
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .obtainOne();

                            connection.update()
                                    .table(PlayerTable.TABLE_NAME)
                                    .set(economy.getName(), result.get().getInt(economy.getName()) - amount)
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .execute();
                        }

                        @Override
                        public void setBalance(GamePlayer gamePlayer, int balance) {
                            connection.update()
                                    .table(PlayerTable.TABLE_NAME)
                                    .set(economy.getName(), balance)
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .execute();
                        }

                        @Override
                        public int getBalance(GamePlayer gamePlayer) {
                            Optional<Row> result = connection.select()
                                    .from(PlayerTable.TABLE_NAME)
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .obtainOne();
                            return result.get().getInt(economy.getName());
                        }
                    });
                }
            }
        }

        if (somethingwrong) {
            Logger.log("I can't register the minigame due to previous problems!", Logger.LogType.ERROR);
            return;
        }else{

            minigameTable.createTable();
            playerTable.createTable();

            Logger.log("Minigame successfully registered!", Logger.LogType.INFO);
            this.minigame = minigame;
        }
    }

    public static boolean useDecentHolograms() {
        return useDecentHolograms;
    }

    public File getMinigameDataFolder(){
        return getMinigame().getPlugin().getDataFolder();
    }
}
