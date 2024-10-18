package cz.johnslovakia.gameapi;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
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
import cz.johnslovakia.gameapi.messages.Language;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.quests.QuestManager;
import cz.johnslovakia.gameapi.users.stats.StatsManager;
import cz.johnslovakia.gameapi.utils.BetterInvisibility;
import cz.johnslovakia.gameapi.utils.BukkitSerialization;
import cz.johnslovakia.gameapi.utils.InputStreamWithName;
import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import lombok.Setter;
import me.zort.containr.Containr;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.Row;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.FileUtil;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarFile;

@Getter
public class GameAPI extends JavaPlugin {

    @Getter
    public static GameAPI instance;
    private Minigame minigame;

    @Setter
    private KitManager kitManager;
    @Setter
    private CosmeticsManager cosmeticsManager;
    @Setter
    private PerkManager perkManager;
    @Setter
    private StatsManager statsManager;
    @Setter
    private QuestManager questManager;


    private String version;
    private VersionSupport versionSupport;
    private UserInterface userInterface;
    private SlimeWorldLoader slimeWorldLoader;
    private Schematic schematicHandler;
    public static boolean useDecentHolograms;

    private Chat vaultChat;
    private Permission vaultPerms;
    private Economy vaultEconomy;
    private ProtocolManager protocolManager;

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



        //VERSION SUPPORT //TODO: opravit pro další verze
        String packageName = this.getServer().getClass().getPackage().getName();
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

        protocolManager = ProtocolLibrary.getProtocolManager();
    }

    @Override
    public void onDisable() {
        getMinigame().getDatabase().disconnect();
    }


    public void registerMinigame(Minigame minigame){
        this.minigame = minigame;

        boolean somethingwrong = false;
        try{
            minigame.setupGames();
            Logger.log("Games successfully loaded!", Logger.LogType.INFO);
        }catch (Exception e){
            Logger.log("Something went wrong when loading the maps! The following message is for Developers: ", Logger.LogType.ERROR);
            e.printStackTrace();
            somethingwrong = true;
        }
        minigame.setupPlayerScores();
        try{
            minigame.setupOther();
        }catch (Exception e){
            Logger.log("Something went wrong when setting up the other necessities for the minigame! The following message is for Developers: ", Logger.LogType.ERROR);
            e.printStackTrace();
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


        File pluginLanguagesFolder = new File(minigame.getPlugin().getDataFolder(), "languages");

        if (!pluginLanguagesFolder.exists()) {
            pluginLanguagesFolder.mkdirs();
        }

        try {
            for (InputStreamWithName is : minigame.getLanguageFiles()/*languagesDir.listFiles()*/) {
                File file = Files.createTempFile("tempfiles", ".yml").toFile();
                FileUtils.copyInputStreamToFile(is.getInputStream(), file);

                String name = is.getFileName();
                File c = new File(pluginLanguagesFolder, name);
                if (c.exists()) {
                    loadMessagesFromFile(c);
                    continue;
                }

                InputStream gapiFile = getResource("languages/" + name);
                if (gapiFile == null) {
                    File cFile = new File(minigame.getPlugin().getDataFolder(), name);
                    if (!cFile.exists()) {
                        minigame.getPlugin().saveResource("languages/" + name, false);
                    }
                    continue;
                }


                minigame.getPlugin().saveResource("languages/" + name, false);
                File createdFile = new File(pluginLanguagesFolder, name);

                File gFile = Files.createTempFile("gFile", ".yml").toFile();
                FileUtils.copyInputStreamToFile(gapiFile, gFile);
                try (BufferedReader br = new BufferedReader(new FileReader(gFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        FileWriter writer = new FileWriter(createdFile, true);
                        writer.append(line).append("\n");
                        writer.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                loadMessagesFromFile(createdFile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (somethingwrong) {
            Logger.log("I can't register the minigame due to previous problems!", Logger.LogType.ERROR);
        }else{

            minigameTable.createTable();
            playerTable.createTable();

            Logger.log("Minigame successfully registered!", Logger.LogType.INFO);
        }


        if (getKitManager() != null){
            Bukkit.getPluginManager().registerEvents(getKitManager(), this);
        }
        if (getCosmeticsManager() != null){
            Bukkit.getPluginManager().registerEvents(getCosmeticsManager(), this);
        }
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
            getLogger().warning("Nastala chyba při načítání souboru " + file.getName() + ": " + e.getMessage());
        }
    }

    public static boolean useDecentHolograms() {
        return useDecentHolograms;
    }

    public File getMinigameDataFolder(){
        return getMinigame().getPlugin().getDataFolder();
    }
}
