package cz.johnslovakia.gameapi.users;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.datastorage.*;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.achievements.Achievement;
import cz.johnslovakia.gameapi.users.achievements.PlayerAchievementData;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.game.kit.KitManager;
import cz.johnslovakia.gameapi.game.perk.Perk;
import cz.johnslovakia.gameapi.game.perk.PerkLevel;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.game.kit.Kit;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.messages.Language;
import cz.johnslovakia.gameapi.users.quests.PlayerQuestData;
import cz.johnslovakia.gameapi.users.quests.Quest;
import cz.johnslovakia.gameapi.users.quests.QuestType;
import cz.johnslovakia.gameapi.users.stats.PlayerStat;
import cz.johnslovakia.gameapi.users.stats.Stat;
import cz.johnslovakia.gameapi.utils.BukkitSerialization;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.inventoryBuilder.InventoryManager;
import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.Row;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

@Getter @Setter
public class PlayerData {

    private final GamePlayer gamePlayer;

    private Game game;
    private GameTeam team;
    private Kit kit;
    private String prefix;
    private Language language;
    private BossBar currentBossBar;
    private KillMessage killMessage;

    private List<GameMap> votesForMaps = new ArrayList<>();

    private Map<CosmeticsCategory, Cosmetic> selectedCosmetics = new HashMap<>();
    private List<Cosmetic> purchasedCosmetics = new ArrayList<>();

    private List<Kit> purchasedKitsThisGame = new ArrayList<>();
    private Map<Kit, Inventory> kitInventories = new HashMap<>();
    private Kit defaultKit;
    private InventoryManager currentInventory;

    private List<PlayerQuestData> questData = new ArrayList<>();

    private List<PlayerAchievementData> achievementData = new ArrayList<>();

    private Map<Perk, Integer> perksLevel = new HashMap<>();


    public PlayerData(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;

        new BukkitRunnable(){
            @Override
            public void run() {
                PlayerTable playerTable = new PlayerTable(gamePlayer);
                playerTable.newUser(gamePlayer);

                GameAPI.getInstance().getMinigame().getMinigameTable().newUser(gamePlayer);
                GameAPI.getInstance().getStatsManager().getStatsTable().newUser(gamePlayer);
                loadData();
            }
        }.runTaskAsynchronously(GameAPI.getInstance());


        try {
            if (GameAPI.getInstance().getMinigame().getDatabase() == null){
                Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
                return;
            }
            Optional<Row> result = GameAPI.getInstance().getMinigame().getDatabase().getConnection().select()
                    .from(PlayerTable.TABLE_NAME)
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .obtainOne();

            if (result.isPresent()){
                String lang = result.get().getString("Language");
                language = Language.getLanguage(lang);
            }else{
                language = Language.getDefaultLanguage();
            }
        } catch (Exception e) {
            language = Language.getDefaultLanguage();
            e.printStackTrace();
        }

    }




    public void loadData(){
        new BukkitRunnable(){
            @Override
            public void run() {
                loadKits();
                loadCosmetics();
                loadPerks();
                loadQuests();
                loadAchievements();

                if (GameAPI.getInstance().getQuestManager() != null) {
                    GameAPI.getInstance().getQuestManager().check(gamePlayer);
                }
            }
        }.runTaskAsynchronously(GameAPI.getInstance());
    }

    public void flushSomeData(){
        game = null;
        team = null;
        kit = defaultKit;
        currentInventory = null;
        votesForMaps.clear();
    }

    public Inventory getKitInventory(Kit kit){
        if (kitInventories.get(kit) == null){
            return kit.getContent().getInventory();
        }else{
            if (!areItemsMatching(kit.getContent().getInventory(), kitInventories.get(kit))){
                kitInventories.remove(kit);
                Bukkit.getScheduler().runTaskAsynchronously(GameAPI.getInstance(), task -> saveKitInventories());

                MessageManager.get(gamePlayer, "chat.kit_layout_reset")
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

    public PlayerData setLanguage(Language language) {
        this.language = language;
        new BukkitRunnable(){
            @Override
            public void run() {
                SQLDatabaseConnection connection = GameAPI.getInstance().getMinigame().getDatabase().getConnection();
                connection.update()
                        .table(PlayerTable.TABLE_NAME)
                        .set("Language", language.getName())
                        .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                        .execute();
            }
        }.runTaskAsynchronously(GameAPI.getInstance());
        return this;
    }

    public PlayerData setDefaultKit(Kit defaultKit) {
        this.defaultKit = defaultKit;
        new BukkitRunnable(){
            @Override
            public void run() {
                SQLDatabaseConnection connection = GameAPI.getInstance().getMinigame().getDatabase().getConnection();
                connection.update()
                        .table(GameAPI.getInstance().getMinigame().getMinigameTable().getTableName())
                        .set("DefaultKit", defaultKit.getName())
                        .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                        .execute();
            }
        }.runTaskAsynchronously(GameAPI.getInstance());
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
        List<Quest> list = new ArrayList<>();
        for (PlayerQuestData data : getQuestData()){
            list.add(data.getQuest());
        }
        return list;
    }

    public List<Quest> getQuestsByStatus(PlayerQuestData.Status status){
        List<Quest> list = new ArrayList<>();
        for (PlayerQuestData data : getQuestData().stream().filter(data -> data.getStatus().equals(status)).toList()){
            list.add(data.getQuest());
        }
        return list;
    }

    public List<PlayerQuestData> getQuestDataByStatus(PlayerQuestData.Status status){
        return new ArrayList<>(getQuestData().stream().filter(data -> data.getStatus().equals(status)).toList());
    }

    private void loadQuests(){
        if (GameAPI.getInstance().getQuestManager() == null){
            return;
        }

        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        Optional<Row> result = connection.select()
                .from(minigame.getMinigameTable().getTableName())
                .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                .obtainOne();
        if (!result.isPresent()) {
            Logger.log("I can't get quests data for player " + gamePlayer.getOnlinePlayer().getName() + ". (1)", Logger.LogType.ERROR);
            gamePlayer.getOnlinePlayer().sendMessage("Can't get your quests data. Sorry for the inconvenience. (1)");
        }else {
            try{
                String jsonString = result.get().getString("Quests");
                if (jsonString != null) {
                    JSONObject jsonObject = new JSONObject(jsonString);

                    JSONArray questsArray = jsonObject.getJSONArray("quests");

                    for (int i = 0; i < questsArray.length(); i++) {
                        JSONObject questObject = questsArray.getJSONObject(i);
                        String name = questObject.getString("name");
                        QuestType type = QuestType.valueOf(questObject.getString("type").toUpperCase());
                        PlayerQuestData.Status status = PlayerQuestData.Status.valueOf(questObject.getString("status").toUpperCase());  // "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
                        int progress = questObject.getInt("progress");
                        LocalDate completionDate = null;
                        if (questObject.has("completion_date") && questObject.get("completion_date") != null) {
                            completionDate = LocalDate.parse(questObject.getString("completion_date"));
                        }

                        Quest quest = GameAPI.getInstance().getQuestManager().getQuest(type, name);
                        if (quest == null){
                            continue;
                        }

                        PlayerQuestData questData;
                        if (status.equals(PlayerQuestData.Status.COMPLETED)) {
                            questData = new PlayerQuestData(quest, gamePlayer, completionDate);
                        } else if (status.equals(PlayerQuestData.Status.IN_PROGRESS)) {
                            questData = new PlayerQuestData(quest, gamePlayer, progress);
                        }else{
                            questData = new PlayerQuestData(quest, gamePlayer);
                        }
                        if (progress >= quest.getCompletionGoal() && status != PlayerQuestData.Status.COMPLETED){
                            Bukkit.getLogger().log(Level.WARNING, "Quest data is incorrectly stored in the database. Status: " + status.name() + " Progress: " + progress + " Completion goal: " + quest.getCompletionGoal() + " Quest Name: " + quest.getDisplayName());
                            questData.setStatus(PlayerQuestData.Status.COMPLETED);
                        }

                        getQuestData().add(questData);
                    }
                }
            } catch (Exception exception) {
                Logger.log("I can't get quests data for player " + gamePlayer.getOnlinePlayer().getName() + ". (2) The following message is for Developers: " + exception.getMessage(), Logger.LogType.ERROR);
                gamePlayer.getOnlinePlayer().sendMessage("Can't get your quests data. Sorry for the inconvenience. (2)");
                exception.printStackTrace();
            }
        }
    }

    private void loadAchievements(){
        if (GameAPI.getInstance().getAchievementManager() == null){
            return;
        }

        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        Optional<Row> result = connection.select()
                .from(minigame.getMinigameTable().getTableName())
                .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                .obtainOne();
        if (!result.isPresent()) {
            Logger.log("I can't get achievements data for player " + gamePlayer.getOnlinePlayer().getName() + ". (1)", Logger.LogType.ERROR);
            gamePlayer.getOnlinePlayer().sendMessage("Can't get your achievements data. Sorry for the inconvenience. (1)");
        }else {
            try{
                String jsonString = result.get().getString("Achievements");
                if (jsonString != null) {
                    JSONObject jsonObject = new JSONObject(jsonString);

                    JSONArray achievementsArray = jsonObject.getJSONArray("Achievements");

                    for (int i = 0; i < achievementsArray.length(); i++) {
                        JSONObject achievementObject = achievementsArray.getJSONObject(i);
                        String name = achievementObject.getString("name");
                        PlayerAchievementData.Status status = PlayerAchievementData.Status.valueOf(achievementObject.getString("status").toUpperCase());  // "LOCKED", "UNLOCKED"
                        int progress = achievementObject.getInt("progress");

                        Achievement achievement = GameAPI.getInstance().getAchievementManager().getAchievement(name);
                        if (achievement == null){
                            continue;
                        }

                        PlayerAchievementData achievementData;
                        if (status.equals(PlayerAchievementData.Status.UNLOCKED)) {
                            achievementData = new PlayerAchievementData(achievement, gamePlayer, PlayerAchievementData.Status.UNLOCKED);
                        }else{
                            achievementData = new PlayerAchievementData(achievement, gamePlayer, progress);
                        }
                        if (progress >= achievement.getCompletionGoal() && status != PlayerAchievementData.Status.UNLOCKED){
                            Bukkit.getLogger().log(Level.WARNING, "Achievement data is incorrectly stored in the database. Status: " + status.name() + " Progress: " + progress + " Completion goal: " + achievement.getCompletionGoal() + " Achievement Name: " + achievement.getDisplayName());
                            achievementData.setStatus(PlayerAchievementData.Status.UNLOCKED);
                        }

                        getAchievementData().add(achievementData);
                    }
                }
            } catch (Exception exception) {
                Logger.log("I can't get achievements data for player " + gamePlayer.getOnlinePlayer().getName() + ". (2) The following message is for Developers: " + exception.getMessage(), Logger.LogType.ERROR);
                gamePlayer.getOnlinePlayer().sendMessage("Can't get your achievements data. Sorry for the inconvenience. (2)");
                exception.printStackTrace();
            }
        }
    }

    public List<PlayerAchievementData> getAchievementsDataByStatus(PlayerAchievementData.Status status){
        PlayerData playerData = gamePlayer.getPlayerData();

        return playerData.getAchievementData().stream().filter(a -> a.getStatus().equals(status)).toList();
    }

    public Optional<PlayerAchievementData> getAchievementData(Achievement achievement){
        return getAchievementData().stream().filter(a -> a.getAchievement().equals(achievement)).findFirst();
    }


    public void setPerkLevel(Perk perk, int level){
        perksLevel.put(perk, level);
    }

    public PerkLevel getPerkLevel(Perk perk){
        if (perksLevel.containsKey(perk)){
            return perk.getLevels().stream().filter(l -> l.level() == perksLevel.get(perk)).toList().get(0);
        }
        return null;
    }

    public boolean hasPerk(Perk perk){
        return getPerkLevel(perk) != null;
    }

    private void loadPerks(){
        if (GameAPI.getInstance().getPerkManager() == null){
            return;
        }

        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        Optional<Row> result = connection.select()
                .from(minigame.getMinigameTable().getTableName())
                .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                .obtainOne();
        if (result.isEmpty()) {
            Logger.log("I can't get perks data for player " + gamePlayer.getOnlinePlayer().getName() + ". (1)", Logger.LogType.ERROR);
            gamePlayer.getOnlinePlayer().sendMessage("Can't get your perks data. Sorry for the inconvenience. (1)");
        }else {
            try{
                String jsonString = result.get().getString("Perks");
                if (jsonString != null) {
                    JSONObject jsonObject = new JSONObject(jsonString);

                    JSONArray perksArray = jsonObject.getJSONArray("levels");

                    for (int i = 0; i < perksArray.length(); i++) {
                        JSONObject perkObject = perksArray.getJSONObject(i);
                        String name = perkObject.getString("name");
                        int level = perkObject.getInt("level");
                        Perk perk = GameAPI.getInstance().getPerkManager().getPerk(name);

                        if (perk != null) {
                            perksLevel.put(perk, level);
                        }
                    }
                }
            } catch (Exception exception) {
                Logger.log("I can't get perks data for player " + gamePlayer.getOnlinePlayer().getName() + ". (2) The following message is for Developers: " + exception.getMessage(), Logger.LogType.ERROR);
                gamePlayer.getOnlinePlayer().sendMessage("Can't get your perks data. Sorry for the inconvenience. (2)");
            }
        }
    }

    public void setKitInventory(Kit kit, Inventory inventory){
        kitInventories.put(kit, inventory);
    }

    private void loadKits(){
        if (game == null){
            return;
        }
        KitManager kitManager = KitManager.getKitManager(game);
        if (kitManager == null){
            return;
        }
        if (kitManager.getDefaultKit() != null){
            Bukkit.getScheduler().runTask(GameAPI.getInstance(), () -> kitManager.getDefaultKit().select(gamePlayer));
        }

        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        Optional<Row> result = connection.select()
                .from(minigame.getMinigameTable().getTableName())
                .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                .obtainOne();
        if (result.isEmpty()) {
            Logger.log("I can't get kits data for player " + gamePlayer.getOnlinePlayer().getName() + ". (1)", Logger.LogType.ERROR);
            gamePlayer.getOnlinePlayer().sendMessage("Can't get your kits data. Sorry for the inconvenience. (1)");
        }else {
            try{
                if (minigame.getSettings().isAllowDefaultKitSelection()) {
                    String rDKit = result.get().getString("DefaultKit");
                    if (rDKit != null) {
                        Kit dKit = kitManager.getKit(result.get().getString("DefaultKit"));
                        if (dKit != null) {
                            this.defaultKit = dKit;

                            Bukkit.getScheduler().runTask(GameAPI.getInstance(), () -> {
                                defaultKit.select(gamePlayer);
                            });
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

                            if (kitManager.getGameMap() != null) {
                                if (kitObject.get("map") == null) continue;
                                if (!kitObject.getString("map").equalsIgnoreCase(kitManager.getGameMap()))
                                    continue;
                            }

                            String name = kitObject.getString("name");
                            String inventory = kitObject.getString("inventory");
                            Kit k = kitManager.getKit(name);

                            if (k != null) {
                                kitInventories.put(k, BukkitSerialization.playerInventoryFromBase64(inventory));
                            }
                        }
                    }else{
                        String map = kitManager.getGameMap();

                        JSONObject kitsData = new JSONObject(jsonString);
                        JSONObject kitsDataJson = kitsData.getJSONObject("kits_data");
                        JSONObject mapsJson = kitsDataJson.getJSONObject("maps");

                        if (mapsJson.has(map)) {
                            JSONObject mapJson = mapsJson.getJSONObject(map);

                            if (mapJson.has("inventories")) {
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
                    }
                }
            } catch (Exception exception) {
                Logger.log("I can't get kits data for player " + gamePlayer.getOnlinePlayer().getName() + ". (2) The following message is for Developers: " + exception.getMessage(), Logger.LogType.ERROR);
                gamePlayer.getOnlinePlayer().sendMessage("Can't get your kits data. Sorry for the inconvenience. (2)");
            }
        }
    }

    public void addPurchasedKitThisGame(Kit kit){
        if (purchasedKitsThisGame.contains(kit)){
            return;
        }
        purchasedKitsThisGame.add(kit);
    }

    public void saveAll(){
        //TODO: změna jen, když se něco změní?
        saveCosmetics();
        saveQuests();
        saveAchievements();
        savePerks();
        saveKitInventories();

        for (Stat stat : GameAPI.getInstance().getStatsManager().getStats()){
            PlayerStat playerStat = stat.getPlayerStat(gamePlayer);
            if (playerStat != null && playerStat.wasUpdated()){
                playerStat.saveDataToDatabase();
            }
        }
    }

    public void saveKitInventories(){
        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        QueryResult kitInventoriesResult = connection.update()
                .table(minigame.getMinigameTable().getTableName())
                .set("KitInventories", KitsStorage.inventoriesToJSON(getGamePlayer()).toString())
                .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                .execute();
        if (!kitInventoriesResult.isSuccessful()) {
            Logger.log("Something went wrong when saving kits data! The following message is for Developers: ", Logger.LogType.ERROR);
            Logger.log(kitInventoriesResult.getRejectMessage(), Logger.LogType.ERROR);
        }
    }

    public void savePerks(){
        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        if (GameAPI.getInstance().getPerkManager() != null) {
            QueryResult perksResult = connection.update()
                    .table(minigame.getMinigameTable().getTableName())
                    .set("Perks", PerksStorage.perksToJSON(getGamePlayer()).toString())
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .execute();
            if (!perksResult.isSuccessful()) {
                Logger.log("Something went wrong when saving perks data! The following message is for Developers: ", Logger.LogType.ERROR);
                Logger.log(perksResult.getRejectMessage(), Logger.LogType.ERROR);
            }
        }
    }

    public void saveQuests(){
        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        if (GameAPI.getInstance().getQuestManager() != null) {
            QueryResult questsResult = connection.update()
                    .table(minigame.getMinigameTable().getTableName())
                    .set("Quests", QuestsStorage.toJSON(getGamePlayer()).toString())
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .execute();
            if (!questsResult.isSuccessful()) {
                Logger.log("Something went wrong when saving quests data! The following message is for Developers: ", Logger.LogType.ERROR);
                Logger.log(questsResult.getRejectMessage(), Logger.LogType.ERROR);
            }
        }
    }

    public void saveAchievements(){
        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        if (GameAPI.getInstance().getAchievementManager() != null) {
            QueryResult achievementsResult = connection.update()
                    .table(minigame.getMinigameTable().getTableName())
                    .set("Achievements", AchievementsStorage.toJSON(getGamePlayer()).toString())
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .execute();
            if (!achievementsResult.isSuccessful()) {
                Logger.log("Something went wrong when saving achievements data! The following message is for Developers: ", Logger.LogType.ERROR);
                Logger.log(achievementsResult.getRejectMessage(), Logger.LogType.ERROR);
            }
        }
    }

    public void saveCosmetics(){
        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        if (GameAPI.getInstance().getCosmeticsManager() != null) {
            QueryResult cosmeticResult = connection.update()
                    .table(minigame.getMinigameTable().getTableName())
                    .set("Cosmetics", CosmeticsStorage.toJSON(getGamePlayer()).toString())
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .execute();
            if (!cosmeticResult.isSuccessful()) {
                Logger.log("Something went wrong when saving cosmetics data! The following message is for Developers: ", Logger.LogType.ERROR);
                Logger.log(cosmeticResult.getRejectMessage(), Logger.LogType.ERROR);
            }
        }
    }



    private void loadCosmetics(){
        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        if (GameAPI.getInstance().getCosmeticsManager() == null){
            return;
        }

        Optional<Row> result = connection.select()
                .from(minigame.getMinigameTable().getTableName())
                .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                .obtainOne();
        if (!result.isPresent()) {
            Logger.log("I can't get cosmetics data for player " + gamePlayer.getOnlinePlayer().getName() + ". (1)", Logger.LogType.ERROR);
            gamePlayer.getOnlinePlayer().sendMessage("Can't get your cosmetics data. Sorry for the inconvenience. (1)");
        }else {
            try{
                String jsonString = result.get().getString("Cosmetics");
                if (jsonString != null) {
                    JSONObject jsonObject = new JSONObject(jsonString);

                    for (CosmeticsCategory category : GameAPI.getInstance().getCosmeticsManager().getCategories()) {
                        if (!jsonObject.getJSONArray("purchased").isEmpty())
                            purchasedCosmetics = CosmeticsStorage.parseJsonArrayToList(jsonObject.getJSONArray("purchased"));


                        if (!jsonObject.getJSONArray("selected").isEmpty()) {
                            List<Cosmetic> cosmetics = CosmeticsStorage.parseJsonArrayToList(jsonObject.getJSONArray("selected")).stream().filter(c -> GameAPI.getInstance().getCosmeticsManager().getCategory(c) != null && GameAPI.getInstance().getCosmeticsManager().getCategory(c).equals(category)).toList();
                            if (!cosmetics.isEmpty()) {
                                //selectedCosmetics.put(category, cosmetics.get(0));
                                cosmetics.get(0).select(gamePlayer, false);
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                Logger.log("I can't get cosmetics data for player " + gamePlayer.getOnlinePlayer().getName() + ". (2) The following message is for Developers: " + exception.getMessage(), Logger.LogType.ERROR);
                gamePlayer.getOnlinePlayer().sendMessage("Can't get your cosmetics data. Sorry for the inconvenience. (2)");
                exception.printStackTrace();
            }
        }
    }

    public void selectCosmetic(Cosmetic cosmetic){
        if (GameAPI.getInstance().getCosmeticsManager().getCategory(cosmetic) != null) {
            if (!selectedCosmetics.containsValue(cosmetic))
                selectedCosmetics.put(GameAPI.getInstance().getCosmeticsManager().getCategory(cosmetic), cosmetic);
        }
    }

    public void purchaseCosmetic(Cosmetic cosmetic){
        if (!purchasedCosmetics.contains(cosmetic)) {
            purchasedCosmetics.add(cosmetic);
        }
    }

    public void removeVotesForMaps(){
        for (GameMap map : getVotesForMaps()){
            map.removeVote();
        }
    }

    public void addVoteForMap(GameMap map){
        votesForMaps.add(map);
    }

    public PlayerStat getStat(Stat stat){
        return stat.getPlayerStat(gamePlayer);
    }

    public PlayerStat getStat(String statName){
        return GameAPI.getInstance().getStatsManager().getStat(statName).getPlayerStat(gamePlayer);
    }


    public void deposit(Resource resource, int amount) {
        if (resource != null) {
            resource.getResourceInterface().deposit(gamePlayer, amount);
        }
    }

    public void withdraw(Resource resource, int amount) {
        if (resource != null) {
            resource.getResourceInterface().withdraw(gamePlayer, amount);
        }
    }

    public int getBalance(Resource resource) {
        if (resource != null) {
            return resource.getResourceInterface().getBalance(gamePlayer);
        }
        return 0;
    }
}
