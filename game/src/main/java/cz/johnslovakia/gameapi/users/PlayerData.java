package cz.johnslovakia.gameapi.users;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.database.PlayerTable;
import cz.johnslovakia.gameapi.modules.kits.Kit;
import cz.johnslovakia.gameapi.modules.kits.KitManager;
import cz.johnslovakia.gameapi.modules.perks.Perk;
import cz.johnslovakia.gameapi.modules.perks.PerkLevel;
import cz.johnslovakia.gameapi.modules.quests.PlayerQuestData;
import cz.johnslovakia.gameapi.modules.quests.Quest;
import cz.johnslovakia.gameapi.modules.quests.QuestType;
import cz.johnslovakia.gameapi.modules.stats.StatsModule;
import cz.johnslovakia.gameapi.utils.BukkitSerialization;
import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.utils.Logger;

import cz.johnslovakia.gameapi.datastorage.KitsStorage;
import cz.johnslovakia.gameapi.datastorage.PerksStorage;
import cz.johnslovakia.gameapi.datastorage.QuestsStorage;
import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.Row;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.*;

@Getter @Setter
public class PlayerData {

    private final GamePlayer gamePlayer;

    private List<Kit> purchasedKitsThisGame = new ArrayList<>();
    private Map<Kit, Inventory> kitInventories = new HashMap<>();
    private Kit defaultKit;

    private List<PlayerQuestData> questData = new ArrayList<>();

    private Map<Perk, Integer> perksLevel = new HashMap<>();


    public PlayerData(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;

        if (Minigame.getInstance().getDatabase() == null){
            return;
        }

        new BukkitRunnable(){
            @Override
            public void run() {
                PlayerTable playerTable = new PlayerTable();

                playerTable.newUser(gamePlayer);
                Minigame.getInstance().getMinigameTable().newUser(gamePlayer);
                ModuleManager.getModule(StatsModule.class).getStatsTable().newUser(gamePlayer);

                loadData();
            }
        }.runTaskAsynchronously(Minigame.getInstance().getPlugin());
    }
    public void loadData(){
        loadPerks();
        loadQuests();
    }

    public Inventory getKitInventory(Kit kit){
        if (kitInventories.get(kit) == null){
            return kit.getContent().getInventory();
        }else{
            if (!areItemsMatching(kit.getContent().getInventory(), kitInventories.get(kit))){
                kitInventories.remove(kit);
                Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> saveKitInventories());

                ModuleManager.getModule(MessageModule.class).get(gamePlayer, "chat.kit_layout_reset")
                        .replace("%kit%", kit.getName())
                        .send();
                return kit.getContent().getInventory();
            }

            return kitInventories.get(kit);
        }
    }

    public boolean areItemsMatching(Inventory defaultKit, Inventory playerLayout) {
        Map<String, Integer> defaultItems = getItemCounts(defaultKit);
        Map<String, Integer> playerItems = getItemCounts(playerLayout);

        return defaultItems.equals(playerItems);
    }

    private Map<String, Integer> getItemCounts(Inventory inventory) {
        Map<String, Integer> itemCounts = new HashMap<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                String itemName = item.getType().toString();
                itemCounts.put(itemName, itemCounts.getOrDefault(itemName, 0) + item.getAmount());
            }
        }
        return itemCounts;
    }

    public PlayerData setDefaultKit(Kit defaultKit) {
        this.defaultKit = defaultKit;

        new BukkitRunnable() {
            @Override
            public void run() {
                try (SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection()) {
                    if (connection == null) return;

                    connection.update()
                            .table(Minigame.getInstance().getMinigameTable().getTableName())
                            .set("DefaultKit", defaultKit.getName())
                            .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                            .execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Minigame.getInstance().getPlugin());

        return this;
    }

    public void addQuestProgress(Quest quest){
        for (PlayerQuestData questData : getQuestData()){
            if (questData.getQuest().equals(quest)){
                questData.increaseProgress();
                break;
            }
        }
    }

    public PlayerQuestData getQuestData(Quest quest){
        for (PlayerQuestData questData : getQuestData()){
            if (questData.getQuest().equals(quest)){
                return questData;
            }
        }
        return null;
    }

    public List<Quest> getQuests(){
        return getQuestData().stream()
                .map(PlayerQuestData::getQuest)
                .toList();
    }

    public List<Quest> getQuestsByStatus(PlayerQuestData.Status status){
        return getQuestData().stream()
                .filter(data -> data.getStatus().equals(status))
                .map(PlayerQuestData::getQuest)
                .toList();
    }

    public List<PlayerQuestData> getQuestDataByStatus(PlayerQuestData.Status status){
        return getQuestData().stream().filter(data -> data.getStatus().equals(status)).toList();
    }

    private void loadQuests() {
        if (Minigame.getInstance().getQuestManager() == null) {
            return;
        }

        Minigame minigame = Minigame.getInstance();

        try (SQLDatabaseConnection connection = minigame.getDatabase().getConnection()) {
            if (connection == null) {
                Logger.log("Database connection is null when loading quests for " + gamePlayer.getOnlinePlayer().getName(), Logger.LogType.ERROR);
                return;
            }

            Optional<Row> result = connection.select()
                    .from(minigame.getMinigameTable().getTableName())
                    .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                    .obtainOne();

            if (result.isEmpty()) {
                //Logger.log("I can't get quests data for player " + gamePlayer.getOnlinePlayer().getName() + ". (1)", Logger.LogType.ERROR);
                //gamePlayer.getOnlinePlayer().sendMessage("Can't get your quests data. Sorry for the inconvenience. (1)");
                return;
            }

            try {
                String jsonString = result.get().getString("Quests");
                if (jsonString != null) {
                    JSONObject jsonObject = new JSONObject(jsonString);
                    JSONArray questsArray = jsonObject.getJSONArray("quests");

                    for (int i = 0; i < questsArray.length(); i++) {
                        JSONObject questObject = questsArray.getJSONObject(i);
                        String name = questObject.getString("name");
                        QuestType type = QuestType.valueOf(questObject.getString("type").toUpperCase());
                        PlayerQuestData.Status status = (!questObject.getString("status").equalsIgnoreCase("NOT_STARTED")
                                ? PlayerQuestData.Status.valueOf(questObject.getString("status").toUpperCase())
                                : PlayerQuestData.Status.IN_PROGRESS);
                        int progress = questObject.getInt("progress");

                        LocalDate startDate = null;
                        if (questObject.has("start_date") && questObject.get("start_date") != null) {
                            startDate = LocalDate.parse(questObject.getString("start_date"));
                        }

                        Quest quest = Minigame.getInstance().getQuestManager().getQuest(type, name);
                        if (quest == null) continue;

                        PlayerQuestData questData = status.equals(PlayerQuestData.Status.COMPLETED)
                                ? new PlayerQuestData(gamePlayer, quest, startDate, PlayerQuestData.Status.COMPLETED)
                                : new PlayerQuestData(gamePlayer, quest, startDate, progress);

                        if (progress >= quest.getCompletionGoal() && status != PlayerQuestData.Status.COMPLETED) {
                            Logger.log("Quest data is incorrectly stored in the database. Status: " + status.name()
                                    + " Progress: " + progress + " Completion goal: " + quest.getCompletionGoal()
                                    + " Quest Name: " + quest.getDisplayName(), Logger.LogType.WARNING);
                            questData.setStatus(PlayerQuestData.Status.COMPLETED);
                        }

                        getQuestData().add(questData);
                    }
                }
            } catch (Exception exception) {
                Logger.log("I can't get quests data for player " + gamePlayer.getOnlinePlayer().getName()
                        + ". (2) The following message is for Developers: " + exception.getMessage(), Logger.LogType.ERROR);
                gamePlayer.getOnlinePlayer().sendMessage("Can't get your quests data. Sorry for the inconvenience. (2)");
                exception.printStackTrace();
            }

        } catch (Exception e) {
            Logger.log("Failed to load quests for player " + gamePlayer.getOnlinePlayer().getName()
                    + " due to SQL error: " + e.getMessage(), Logger.LogType.ERROR);
            e.printStackTrace();
        }
    }

    public void setPerkLevel(Perk perk, int level){
        if (perksLevel == null)
            perksLevel = new HashMap<>();

        perksLevel.put(perk, level);
    }

    public PerkLevel getPerkLevel(Perk perk){
        if (perksLevel != null){
            if (perksLevel.containsKey(perk)){
            return perk.getLevels().stream().filter(l -> l.level() == perksLevel.get(perk)).toList().get(0);
            }
        }
        return null;
    }

    public boolean hasPerk(Perk perk){
        return getPerkLevel(perk) != null;
    }

    private void loadPerks() {
        if (Minigame.getInstance().getPerkManager() == null) {
            return;
        }

        Minigame minigame = Minigame.getInstance();

        try (SQLDatabaseConnection connection = minigame.getDatabase().getConnection()) {
            if (connection == null) {
                Logger.log("Database connection is null when loading perks for " + gamePlayer.getOnlinePlayer().getName(), Logger.LogType.ERROR);
                return;
            }

            Optional<Row> result = connection.select()
                    .from(minigame.getMinigameTable().getTableName())
                    .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                    .obtainOne();

            if (!result.isPresent()) return;

            try {
                String jsonString = result.get().getString("Perks");
                if (jsonString != null) {
                    JSONObject jsonObject = new JSONObject(jsonString);
                    JSONArray perksArray = jsonObject.getJSONArray("levels");

                    for (int i = 0; i < perksArray.length(); i++) {
                        JSONObject perkObject = perksArray.getJSONObject(i);
                        String name = perkObject.getString("name");
                        int level = perkObject.getInt("level");
                        Perk perk = minigame.getPerkManager().getPerk(name);

                        if (perk != null) {
                            setPerkLevel(perk, level);
                        }
                    }
                }
            } catch (Exception exception) {
                Logger.log("I can't get perks data for player " + gamePlayer.getOnlinePlayer().getName()
                        + ". (2) The following message is for Developers: " + exception.getMessage(), Logger.LogType.ERROR);
                gamePlayer.getOnlinePlayer().sendMessage("Can't get your perks data. Sorry for the inconvenience. (2)");
                exception.printStackTrace();
            }

        } catch (Exception e) {
            Logger.log("Failed to load perks for player " + gamePlayer.getOnlinePlayer().getName()
                    + " due to SQL error: " + e.getMessage(), Logger.LogType.ERROR);
            e.printStackTrace();
        }
    }

    public void setKitInventory(Kit kit, Inventory inventory){
        kitInventories.put(kit, inventory);
    }

    public void loadKits() {
        if (!getGamePlayer().isInGame()) {
            return;
        }

        KitManager kitManager = KitManager.getKitManager(getGamePlayer().getGame());
        if (kitManager == null) return;

        if (kitManager.getDefaultKit() != null) {
            Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(),
                    () -> kitManager.getDefaultKit().select(gamePlayer));
        }

        Minigame minigame = Minigame.getInstance();

        try (SQLDatabaseConnection connection = minigame.getDatabase().getConnection()) {
            if (connection == null) {
                Logger.log("Database connection is null when loading kits for " + gamePlayer.getOnlinePlayer().getName(), Logger.LogType.ERROR);
                return;
            }

            Optional<Row> result = connection.select()
                    .from(minigame.getMinigameTable().getTableName())
                    .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                    .obtainOne();

            if (!result.isPresent()) return;

            try {
                if (minigame.getSettings().isAllowDefaultKitSelection()) {
                    String rDKit = result.get().getString("DefaultKit");
                    if (rDKit != null) {
                        Kit dKit = kitManager.getKit(rDKit);
                        if (dKit != null) {
                            this.defaultKit = dKit;
                            Bukkit.getScheduler().runTask(minigame.getPlugin(), () -> defaultKit.select(gamePlayer));
                        }
                    }
                }

                String jsonString = result.get().getString("KitInventories");
                if (jsonString != null) {
                    if (kitManager.getGameMap() == null) {
                        JSONObject jsonObject = new JSONObject(jsonString);
                        JSONArray inventoriesArray = jsonObject.getJSONArray("inventories");

                        for (int i = 0; i < inventoriesArray.length(); i++) {
                            JSONObject kitObject = inventoriesArray.getJSONObject(i);

                            if (kitObject.has("map") && !kitObject.getString("map").equalsIgnoreCase(kitManager.getGameMap())) {
                                continue;
                            }

                            String name = kitObject.getString("name");
                            String inventory = kitObject.getString("inventory");
                            Kit k = kitManager.getKit(name);
                            if (k != null) {
                                kitInventories.put(k, BukkitSerialization.playerInventoryFromBase64(inventory));
                            }
                        }
                    } else {
                        String map = kitManager.getGameMap();
                        JSONObject kitsData = new JSONObject(jsonString);
                        JSONObject kitsDataJson = kitsData.optJSONObject("kits_data");
                        if (kitsDataJson == null) return;
                        JSONObject mapsJson = kitsDataJson.optJSONObject("maps");
                        if (mapsJson == null || !mapsJson.has(map)) return;

                        JSONObject mapJson = mapsJson.getJSONObject(map);
                        if (!mapJson.has("inventories")) return;

                        JSONObject kitInventoriesJson = mapJson.getJSONObject("inventories");
                        for (String kitName : kitInventoriesJson.keySet()) {
                            String base64Inventory = kitInventoriesJson.getString(kitName);
                            Kit kit = kitManager.getKit(kitName);
                            if (kit != null) {
                                Inventory inventory = BukkitSerialization.playerInventoryFromBase64(base64Inventory);
                                kitInventories.put(kit, inventory);
                            }
                        }
                    }
                }

            } catch (Exception exception) {
                Logger.log("I can't get kits data for player " + gamePlayer.getOnlinePlayer().getName()
                        + ". The following message is for Developers: " + exception.getMessage(), Logger.LogType.ERROR);
                gamePlayer.getOnlinePlayer().sendMessage("Can't get your kits data. Sorry for the inconvenience.");
                exception.printStackTrace();
            }

        } catch (Exception e) {
            Logger.log("Failed to load kits for player " + gamePlayer.getOnlinePlayer().getName()
                    + " due to SQL error: " + e.getMessage(), Logger.LogType.ERROR);
            e.printStackTrace();
        }
    }

    public void addPurchasedKitThisGame(Kit kit){
        if (purchasedKitsThisGame == null)
            purchasedKitsThisGame = new ArrayList<>();


        if (purchasedKitsThisGame.contains(kit)){
            return;
        }
        purchasedKitsThisGame.add(kit);
    }

    public void saveAll(){
        saveQuests();
        savePerks();
        saveKitInventories();
    }

    public void saveKitInventories() {
        Minigame minigame = Minigame.getInstance();

        try (SQLDatabaseConnection connection = minigame.getDatabase().getConnection()) {
            if (connection == null) {
                Logger.log("Database connection is null when saving kits for " + gamePlayer.getOnlinePlayer().getName(), Logger.LogType.ERROR);
                return;
            }

            QueryResult kitInventoriesResult = connection.update()
                    .table(minigame.getMinigameTable().getTableName())
                    .set("KitInventories", KitsStorage.inventoriesToJSON(getGamePlayer()).toString())
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .execute();

            if (!kitInventoriesResult.isSuccessful()) {
                Logger.log("Something went wrong when saving kits data! The following message is for Developers: ", Logger.LogType.ERROR);
                Logger.log(kitInventoriesResult.getRejectMessage(), Logger.LogType.ERROR);
                gamePlayer.getOnlinePlayer().sendMessage("§cAn error occurred while saving kits data. Sorry for the inconvenience.");
            }

        } catch (Exception e) {
            Logger.log("Failed to save kits data for player " + gamePlayer.getOnlinePlayer().getName()
                    + " due to SQL error: " + e.getMessage(), Logger.LogType.ERROR);
            e.printStackTrace();
            gamePlayer.getOnlinePlayer().sendMessage("§cAn error occurred while saving kits data. Sorry for the inconvenience.");
        }
    }

    public void savePerks() {
        Minigame minigame = Minigame.getInstance();

        if (Minigame.getInstance().getPerkManager() == null) {
            return;
        }

        try (SQLDatabaseConnection connection = minigame.getDatabase().getConnection()) {
            if (connection == null) {
                Logger.log("Database connection is null when saving perks for " + gamePlayer.getOnlinePlayer().getName(), Logger.LogType.ERROR);
                return;
            }

            QueryResult perksResult = connection.update()
                    .table(minigame.getMinigameTable().getTableName())
                    .set("Perks", PerksStorage.perksToJSON(getGamePlayer()).toString())
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .execute();

            if (!perksResult.isSuccessful()) {
                Logger.log("Something went wrong when saving perks data! The following message is for Developers: ", Logger.LogType.ERROR);
                Logger.log(perksResult.getRejectMessage(), Logger.LogType.ERROR);
                gamePlayer.getOnlinePlayer().sendMessage("§cAn error occurred while saving perks data. Sorry for the inconvenience.");
            }

        } catch (Exception e) {
            Logger.log("Failed to save perks data for player " + gamePlayer.getOnlinePlayer().getName()
                    + " due to SQL error: " + e.getMessage(), Logger.LogType.ERROR);
            e.printStackTrace();
            gamePlayer.getOnlinePlayer().sendMessage("§cAn error occurred while saving perks data. Sorry for the inconvenience.");
        }
    }

    public void saveQuests() {
        Minigame minigame = Minigame.getInstance();

        if (Minigame.getInstance().getQuestManager() == null) {
            return;
        }

        try (SQLDatabaseConnection connection = minigame.getDatabase().getConnection()) {
            if (connection == null) {
                Logger.log("Database connection is null when saving quests for " + gamePlayer.getOnlinePlayer().getName(), Logger.LogType.ERROR);
                return;
            }

            QueryResult questsResult = connection.update()
                    .table(minigame.getMinigameTable().getTableName())
                    .set("Quests", QuestsStorage.toJSON(getGamePlayer()).toString())
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .execute();

            if (!questsResult.isSuccessful()) {
                Logger.log("Something went wrong when saving quests data! The following message is for Developers: ", Logger.LogType.ERROR);
                Logger.log(questsResult.getRejectMessage(), Logger.LogType.ERROR);
                gamePlayer.getOnlinePlayer().sendMessage("§cAn error occurred while saving quests data. Sorry for the inconvenience.");
            }

        } catch (Exception e) {
            Logger.log("Failed to save quests data for player " + gamePlayer.getOnlinePlayer().getName()
                    + " due to SQL error: " + e.getMessage(), Logger.LogType.ERROR);
            e.printStackTrace();
            gamePlayer.getOnlinePlayer().sendMessage("§cAn error occurred while saving quests data. Sorry for the inconvenience.");
        }
    }
}
