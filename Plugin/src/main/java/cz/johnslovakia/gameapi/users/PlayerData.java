package cz.johnslovakia.gameapi.users;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.datastorage.*;
import cz.johnslovakia.gameapi.levelSystem.DailyMeter;
import cz.johnslovakia.gameapi.levelSystem.LevelManager;
import cz.johnslovakia.gameapi.levelSystem.LevelProgress;
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
import cz.johnslovakia.gameapi.utils.rewards.Reward;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.DailyMeterUnclaimedReward;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.LevelUpUnclaimedReward;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.QuestUnclaimedReward;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.UnclaimedReward;
import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Getter @Setter
public class PlayerData {

    private final GamePlayer gamePlayer;

    private String prefix = "";
    private Language language = Language.getDefaultLanguage();
    private KillMessage killMessage;
    private int level;
    private int dailyRewardsClaims = 0;

    private List<PlayerScore> scores = new ArrayList<>();
    private List<PlayerStat> stats = new ArrayList<>();
    private List<GameMap> votesForMaps = new ArrayList<>();

    private Map<CosmeticsCategory, Cosmetic> selectedCosmetics = new HashMap<>();
    private List<Cosmetic> purchasedCosmetics = new ArrayList<>();

    private List<UnclaimedReward> unclaimedRewards;// = new ArrayList<>();

    private List<Kit> purchasedKitsThisGame = new ArrayList<>();
    private Map<Kit, Inventory> kitInventories = new HashMap<>();
    private Kit defaultKit;
    private InventoryManager currentInventory;

    private List<PlayerQuestData> questData = new ArrayList<>();

    private List<PlayerAchievementData> achievementData = new ArrayList<>();

    //TODO: psalo to chybu při saving když nebyl hashmap v default
    private Map<Perk, Integer> perksLevel = new HashMap<>();


    public PlayerData(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;

        if (Minigame.getInstance().getDatabase() == null){
            return;
        }


        try {
            Optional<Row> result = Minigame.getInstance().getDatabase().getConnection().select()
                    .from(PlayerTable.TABLE_NAME)
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .obtainOne();

            result.ifPresent(row -> language = Optional.ofNullable(Language.getLanguage(row.getString("Language")))
                    .orElse(Language.getDefaultLanguage()));
        } catch (Exception e) {
            e.printStackTrace();
        }


        new BukkitRunnable(){
            @Override
            public void run() {
                PlayerTable playerTable = new PlayerTable();
                playerTable.newUser(gamePlayer);

                Minigame.getInstance().getMinigameTable().newUser(gamePlayer);
                Minigame.getInstance().getStatsManager().getTable().newUser(gamePlayer);

                loadData();
            }
        }.runTaskAsynchronously(Minigame.getInstance().getPlugin());
    }

    public PlayerStat getPlayerStat(Stat stat){
        Optional<PlayerStat> optionalStat = stats.stream()
                .filter(playerStat -> playerStat.getStat().equals(stat))
                .findFirst();

        if (optionalStat.isPresent()) {
            return optionalStat.get();
        } else {
            PlayerStat playerStat = new PlayerStat(gamePlayer, stat);
            stats.add(playerStat);
            return playerStat;
        }
    }

    public PlayerStat getPlayerStat(String statName){
        Stat stat = Minigame.getInstance().getStatsManager().getStat(statName);

        Optional<PlayerStat> optionalStat = stats.stream()
                .filter(playerStat -> playerStat.getStat().equals(stat))
                .findFirst();

        if (optionalStat.isPresent()) {
            return optionalStat.get();
        } else {
            PlayerStat playerStat = new PlayerStat(gamePlayer, stat);
            stats.add(playerStat);
            return playerStat;
        }
    }

    public void setLanguage(Language language){
        setLanguage(language, false);
    }

    public void setLanguage(Language language, boolean message){
        Language oldLanguage = this.language;
        this.language = language;
        Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> {
            SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
            if (connection == null) {
                return;
            }
            QueryResult result = connection.update()
                    .table(PlayerTable.TABLE_NAME)
                    .set("Language", language.getName())
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .execute();

            if (result.isSuccessful()) {
                this.language = language;
                if (message)
                    MessageManager.get(gamePlayer, "chat.language.changed")
                            .replace("%language%", StringUtils.capitalize(language.getName()))
                            .send();
            } else {
                if (message)
                    gamePlayer.getOnlinePlayer().sendMessage("§cSomething went wrong. I can't change your language.");
                this.language = oldLanguage;
                Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
            }
        });
    }

    //TODO: dát jako async možná
    /*public boolean grantDailyFirstWinReward(){
        String nick = getGamePlayer().getOnlinePlayer().getName();
        Minigame minigame = Minigame.getInstance();

        if (minigame.getDatabase() == null){
            return false;
        }
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();
        if (connection == null){
            return false;
        }

        Optional<Row> result = connection.select()
                .from(Minigame.getInstance().getMinigameTable().getTableName())
                .where().isEqual("Nickname", nick)
                .obtainOne();

        if (result.isPresent()) {
            String lastRewardDateStr = result.get().getString("LastDailyWinReward");
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            boolean shouldReward = true;

            if (lastRewardDateStr != null) {
                try {
                    LocalDate lastRewardDate = LocalDate.parse(lastRewardDateStr, formatter);
                    if (!lastRewardDate.isBefore(today)) {
                        shouldReward = false;
                    }
                } catch (DateTimeParseException ignored) {
                }
            }

            if (shouldReward) {
                String todayStr = today.format(formatter);
                connection.update()
                        .table(minigame.getStatsManager().getTable().getTABLE_NAME())
                        .set("LastDailyWinReward", todayStr)
                        .where().isEqual("Nickname", nick)
                        .execute();
            }
            return shouldReward;
        }
        return false;
    }*/

    public void addUnclaimedReward(UnclaimedReward unclaimedReward){
        if (unclaimedRewards == null)
            unclaimedRewards = new ArrayList<>();

        unclaimedRewards.add(unclaimedReward);
    }

    public List<UnclaimedReward> getUnclaimedRewards(UnclaimedReward.Type type){
        if (unclaimedRewards == null)
            return Collections.emptyList();

        return unclaimedRewards.stream().filter(unclaimedReward -> unclaimedReward.getType().equals(type)).toList();
    }

    public void loadUnclaimedRewards(){
        Minigame minigame = Minigame.getInstance();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        QueryRowsResult<Row> result = connection.select()
                .from("unclaimed_rewards")
                .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                .obtainAll();
        if (!result.isEmpty()){
            try{
                for (Row row : result){
                    LocalDateTime createdAt = null;
                    Object timestampObj = row.get("created_at");
                    if (timestampObj instanceof Timestamp timestamp)
                        createdAt = timestamp.toLocalDateTime();
                    if (createdAt == null)
                        return;
                    String rewardJson = row.getString("reward_json");
                    JsonObject dataJson = JsonParser.parseString(row.getString("data_json")).getAsJsonObject();
                    UnclaimedReward.Type type = UnclaimedReward.Type.valueOf(row.getString("type"));

                    switch (type){
                        case QUEST:
                            QuestUnclaimedReward questUnclaimedReward = new QuestUnclaimedReward(gamePlayer, createdAt, rewardJson, dataJson, type);
                            addUnclaimedReward(questUnclaimedReward);
                            break;
                        case DAILYMETER:
                            DailyMeterUnclaimedReward dailyMeterUnclaimedReward = new DailyMeterUnclaimedReward(gamePlayer, createdAt, rewardJson, dataJson, type);
                            addUnclaimedReward(dailyMeterUnclaimedReward);
                            break;
                        case LEVELUP:
                            LevelUpUnclaimedReward levelUpUnclaimedReward = new LevelUpUnclaimedReward(gamePlayer, createdAt, rewardJson, dataJson, type);
                            addUnclaimedReward(levelUpUnclaimedReward);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown reward type: " + type);
                    }
                }
            } catch (Exception exception) {
                Logger.log("I can't get unclaimed rewards data for player " + gamePlayer.getOnlinePlayer().getName() + ". (2) The following message is for Developers: " + exception.getMessage(), Logger.LogType.ERROR);
            }
        }
        if (getUnclaimedRewards() != null && !getUnclaimedRewards().isEmpty() && getCurrentInventory() != null)
            getCurrentInventory().give(gamePlayer.getOnlinePlayer());
    }


    public void loadData(){
        //loadKits(); //Přesunuto do Game, protože to může zavíset na Game
        loadLevelSystem();
        loadCosmetics();
        loadPerks();
        loadQuests();
        loadAchievements();
        loadUnclaimedRewards();


        if (Minigame.getInstance().getLevelManager() != null) {
            Optional<Row> result = Minigame.getInstance().getDatabase().getConnection().select()
                    .from(PlayerTable.TABLE_NAME)
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .obtainOne();
            result.ifPresent(row -> {
                level = row.getInt("Level");

                LevelProgress levelProgress = Minigame.getInstance().getLevelManager().getLevelProgress(gamePlayer);
                float xpProgress = (float) levelProgress.xpOnCurrentLevel() / levelProgress.levelRange().neededXP();

                Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), task -> {
                    gamePlayer.getOnlinePlayer().setExp(Math.min(xpProgress, 1.0f));
                    gamePlayer.getOnlinePlayer().setLevel(levelProgress.level());
                });
            });
        }
    }

    public void flushSomeData(){
        getGamePlayer().setKit(getDefaultKit());
        currentInventory = null;
        votesForMaps.clear();
    }


    public void setLevel(int newLevel){
        level = newLevel;
        Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> {
            QueryResult kitInventoriesResult = Minigame.getInstance().getDatabase().getConnection().update()
                    .table(PlayerTable.TABLE_NAME)
                    .set("Level", newLevel)
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .execute();
            if (!kitInventoriesResult.isSuccessful()) {
                Logger.log(kitInventoriesResult.getRejectMessage(), Logger.LogType.ERROR);
            }
        });
    }

    public LevelProgress getLevelProgress(){
        return Minigame.getInstance().getLevelManager().getLevelProgress(gamePlayer);
    }

    public int getDailyXP(){
        if (Minigame.getInstance().getDatabase() != null) {
            SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
            String tableName = PlayerTable.TABLE_NAME;

            Optional<Row> result = connection.select()
                    .from(tableName)
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .obtainOne();
            if (result.isPresent()){
                return result.get().getInt("DailyXP");
            }
        }
        return 0;
    }

    public void addDailyXP(int amount){
        if (Minigame.getInstance().getLevelManager() == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> {
            if (Minigame.getInstance().getDatabase() != null) {
                SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
                String tableName = PlayerTable.TABLE_NAME;

                Optional<Row> result = connection.select()
                        .from(tableName)
                        .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                        .obtainOne();

                if (result.isPresent()) {
                    int balance = result.get().getInt("DailyXP");
                    int newBalance = balance + amount;

                    connection.update()
                            .table(tableName)
                            .set("DailyXP",  balance + amount)
                            .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                            .execute();

                    //TODO: sečíst tiery neededXP a prostě to brát podle daily XP prostě


                    List<DailyMeter.DailyMeterTier> tiers = new ArrayList<>(Minigame.getInstance()
                            .getLevelManager()
                            .getDailyMeter()
                            .getTiers()
                            .stream()
                            .filter(t ->
                                    t.tier() > dailyRewardsClaims &&
                                            getUnclaimedRewards(UnclaimedReward.Type.DAILYMETER)
                                                    .stream()
                                                    .map(r -> (DailyMeterUnclaimedReward) r)
                                                    .noneMatch(r -> r.getTier() == t.tier())
                            )
                            .toList());


                    int xp = newBalance;
                    for (DailyMeter.DailyMeterTier tier : Minigame.getInstance()
                            .getLevelManager()
                            .getDailyMeter()
                            .getTiers()) {

                        boolean isClaimedOrUnclaimed = tier.tier() <= dailyRewardsClaims ||
                                getUnclaimedRewards(UnclaimedReward.Type.DAILYMETER)
                                        .stream()
                                        .map(r -> (DailyMeterUnclaimedReward) r)
                                        .anyMatch(r -> r.getTier() == tier.tier());

                        if (isClaimedOrUnclaimed) {
                            xp -= tier.neededXP();
                        } else if (xp >= tier.neededXP()) {
                            Reward reward = tier.reward();

                            JsonObject json = new JsonObject();
                            json.addProperty("tier", tier.tier());
                            reward.setAsClaimable(gamePlayer, UnclaimedReward.Type.DAILYMETER, json);

                            xp -= tier.neededXP();
                        } else {
                            break;
                        }
                    }
                }
            }
        });
    }

    public DailyMeter.DailyMeterTier getDailyMeterTier(){
        /*if (dailyRewardsClaims == 0) {
            if (Minigame.getInstance().getDatabase() != null) {
                SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
                String tableName = PlayerTable.TABLE_NAME;

                Optional<Row> result = connection.select()
                        .from(tableName)
                        .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                        .obtainOne();
                if (result.isPresent()) {
                    int claims = result.get().getInt("DailyRewards_claims");
                    if (claims >= Minigame.getInstance().getLevelManager().getDailyMeter().getMaxTier())
                        return null;
                    return Minigame.getInstance().getLevelManager().getDailyMeter().getTiers().get(claims);
                }
            }
            return null;
        }else{
            if (dailyRewardsClaims >= Minigame.getInstance().getLevelManager().getDailyMeter().getMaxTier())
                return null;
            return Minigame.getInstance().getLevelManager().getDailyMeter().getTiers().get(dailyRewardsClaims);
        }*/
        if (Minigame.getInstance().getLevelManager() == null) return null;

        if (dailyRewardsClaims >= Minigame.getInstance().getLevelManager().getDailyMeter().getMaxTier())
            return null;
        return Minigame.getInstance().getLevelManager().getDailyMeter().getTiers().get(dailyRewardsClaims);
    }

    public void saveDailyRewardsClaims(){
        Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> {
            SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
            String tableName = PlayerTable.TABLE_NAME;

            connection.update()
                    .table(tableName)
                    .set("DailyRewards_claims", dailyRewardsClaims)
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .execute();
        });
    }

    public void loadLevelSystem(){
        if (Minigame.getInstance().getLevelManager() != null && Minigame.getInstance().getDatabase() != null) {
            SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
            String tableName = PlayerTable.TABLE_NAME;

            Optional<Row> result = connection.select()
                    .from(tableName)
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .obtainOne();
            if (result.isPresent()) {
                LocalDate today = LocalDate.now();
                if (result.get().getString("DailyRewards_reset") == null) {
                    connection.update()
                            .table(tableName)
                            .set("DailyRewards_reset", today)
                            .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                            .execute();
                } else {
                    LocalDate lastReset = LocalDate.parse(result.get().getString("DailyRewards_reset"));
                    if (!lastReset.equals(today)) {
                        connection.update()
                                .table(tableName)
                                .set("DailyRewards_reset", today)
                                .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                .execute();
                        connection.update()
                                .table(tableName)
                                .set("DailyXP", 0)
                                .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                .execute();
                        connection.update()
                                .table(tableName)
                                .set("DailyRewards_claims", 0)
                                .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                .execute();
                    } else {
                        dailyRewardsClaims = result.get().getInt("DailyRewards_claims");
                    }
                }
            }
        }
    }

    public void addPlayerScore(PlayerManager.Score score){
        boolean exists = scores.stream()
                .anyMatch(ps -> ps.getName().equalsIgnoreCase(score.getName()));

        if (!exists)
            scores.add(new PlayerScore(gamePlayer, score));
    }

    public void addPlayerScore(PlayerScore playerScore){
        boolean exists = scores.stream()
                .anyMatch(ps -> ps.getName().equalsIgnoreCase(playerScore.getName()));

        if (!exists)
            scores.add(playerScore);
    }

    public Inventory getKitInventory(Kit kit){
        if (kitInventories.get(kit) == null){
            return kit.getContent().getInventory();
        }else{
            if (!areItemsMatching(kit.getContent().getInventory(), kitInventories.get(kit))){
                kitInventories.remove(kit);
                Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task -> saveKitInventories());

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

    public PlayerData setDefaultKit(Kit defaultKit) {
        this.defaultKit = defaultKit;
        new BukkitRunnable(){
            @Override
            public void run() {
                SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
                connection.update()
                        .table(Minigame.getInstance().getMinigameTable().getTableName())
                        .set("DefaultKit", defaultKit.getName())
                        .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                        .execute();
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

    private void loadQuests(){
        if (Minigame.getInstance().getQuestManager() == null){
            return;
        }

        Minigame minigame = Minigame.getInstance();
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
                        PlayerQuestData.Status status = (!questObject.getString("status").equalsIgnoreCase("NOT_STARTED") ? PlayerQuestData.Status.valueOf(questObject.getString("status").toUpperCase()) : PlayerQuestData.Status.IN_PROGRESS);  // "IN_PROGRESS", "COMPLETED"
                        int progress = questObject.getInt("progress");
                        LocalDate startDate = null;
                        if (questObject.has("start_date") && questObject.get("start_date") != null) {
                            startDate = LocalDate.parse(questObject.getString("start_date"));
                        }


                        Quest quest = Minigame.getInstance().getQuestManager().getQuest(type, name);
                        if (quest == null){
                            continue;
                        }

                        PlayerQuestData questData;
                        if (status.equals(PlayerQuestData.Status.COMPLETED)) {
                            questData = new PlayerQuestData(quest, gamePlayer, startDate, PlayerQuestData.Status.COMPLETED);
                        } else {
                            questData = new PlayerQuestData(quest, gamePlayer, startDate, progress);
                        }

                        if (progress >= quest.getCompletionGoal() && status != PlayerQuestData.Status.COMPLETED){
                            Logger.log("Quest data is incorrectly stored in the database. Status: " + status.name() + " Progress: " + progress + " Completion goal: " + quest.getCompletionGoal() + " Quest Name: " + quest.getDisplayName(), Logger.LogType.WARNING);
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
        if (Minigame.getInstance().getAchievementManager() == null){
            return;
        }

        Minigame minigame = Minigame.getInstance();
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
                        int stage = achievementObject.getInt("stage");
                        int progress = achievementObject.getInt("progress");

                        Achievement achievement = Minigame.getInstance().getAchievementManager().getAchievement(name);
                        if (achievement == null){
                            continue;
                        }

                        PlayerAchievementData achievementData;
                        if (status.equals(PlayerAchievementData.Status.UNLOCKED)) {
                            achievementData = new PlayerAchievementData(achievement, gamePlayer, PlayerAchievementData.Status.UNLOCKED);
                        }else{
                            achievementData = new PlayerAchievementData(achievement, gamePlayer, achievement.getStages().get(stage - 1), progress);
                        }
                        /*if (progress >= achievement.get() && status != PlayerAchievementData.Status.UNLOCKED){
                            Bukkit.getLogger().log(Level.WARNING, "Achievement data is incorrectly stored in the database. Status: " + status.name() + " Progress: " + progress + " Completion goal: " + achievement.getCompletionGoal() + " Achievement Name: " + achievement.getDisplayName());
                            achievementData.setStatus(PlayerAchievementData.Status.UNLOCKED);
                        }*/

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

    private void loadPerks(){
        if (Minigame.getInstance().getPerkManager() == null){
            return;
        }

        Minigame minigame = Minigame.getInstance();
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
                        Perk perk = Minigame.getInstance().getPerkManager().getPerk(name);

                        if (perk != null) {
                            setPerkLevel(perk, level);
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

    public void loadKits(){
        if (!getGamePlayer().isInGame()){
            return;
        }
        KitManager kitManager = KitManager.getKitManager(getGamePlayer().getGame());
        if (kitManager == null){
            return;
        }
        if (kitManager.getDefaultKit() != null){
            Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), () -> kitManager.getDefaultKit().select(gamePlayer));
        }

        Minigame minigame = Minigame.getInstance();
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

                            Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), () -> {
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

                        if (kitsDataJson == null){
                            return;
                        }
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
        if (purchasedKitsThisGame == null)
            purchasedKitsThisGame = new ArrayList<>();


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

        for (Stat stat : Minigame.getInstance().getStatsManager().getStats()){
            PlayerStat playerStat = getPlayerStat(stat);
            if (playerStat != null && playerStat.wasUpdated()){
                playerStat.saveDataToDatabase();
            }
        }
    }

    public void saveKitInventories(){
        Minigame minigame = Minigame.getInstance();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        QueryResult kitInventoriesResult = connection.update()
                .table(minigame.getMinigameTable().getTableName())
                .set("KitInventories", KitsStorage.inventoriesToJSON(getGamePlayer()).toString())
                .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                .execute();
        if (!kitInventoriesResult.isSuccessful()) {
            Logger.log("Something went wrong when saving kits data! The following message is for Developers: ", Logger.LogType.ERROR);
            Logger.log(kitInventoriesResult.getRejectMessage(), Logger.LogType.ERROR);
            getGamePlayer().getOnlinePlayer().sendMessage("§cAn error occurred while saving kits data. Sorry for the inconvenience.");
        }
    }

    public void savePerks(){
        Minigame minigame = Minigame.getInstance();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        if (Minigame.getInstance().getPerkManager() != null) {
            QueryResult perksResult = connection.update()
                    .table(minigame.getMinigameTable().getTableName())
                    .set("Perks", PerksStorage.perksToJSON(getGamePlayer()).toString())
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .execute();
            if (!perksResult.isSuccessful()) {
                Logger.log("Something went wrong when saving perks data! The following message is for Developers: ", Logger.LogType.ERROR);
                Logger.log(perksResult.getRejectMessage(), Logger.LogType.ERROR);
                getGamePlayer().getOnlinePlayer().sendMessage("§cAn error occurred while saving perks data. Sorry for the inconvenience.");
            }
        }
    }

    public void saveQuests(){
        Minigame minigame = Minigame.getInstance();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        if (Minigame.getInstance().getQuestManager() != null) {
            QueryResult questsResult = connection.update()
                    .table(minigame.getMinigameTable().getTableName())
                    .set("Quests", QuestsStorage.toJSON(getGamePlayer()).toString())
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .execute();
            if (!questsResult.isSuccessful()) {
                Logger.log("Something went wrong when saving quests data! The following message is for Developers: ", Logger.LogType.ERROR);
                Logger.log(questsResult.getRejectMessage(), Logger.LogType.ERROR);
                getGamePlayer().getOnlinePlayer().sendMessage("§cAn error occurred while saving quests data. Sorry for the inconvenience.");
            }
        }
    }

    public void saveAchievements(){
        Minigame minigame = Minigame.getInstance();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        if (Minigame.getInstance().getAchievementManager() != null) {
            QueryResult achievementsResult = connection.update()
                    .table(minigame.getMinigameTable().getTableName())
                    .set("Achievements", AchievementsStorage.toJSON(getGamePlayer()).toString())
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .execute();
            if (!achievementsResult.isSuccessful()) {
                Logger.log("Something went wrong when saving achievements data! The following message is for Developers: ", Logger.LogType.ERROR);
                Logger.log(achievementsResult.getRejectMessage(), Logger.LogType.ERROR);
                getGamePlayer().getOnlinePlayer().sendMessage("§cAn error occurred while saving achievements data. Sorry for the inconvenience.");
            }
        }
    }

    public void saveCosmetics(){
        Minigame minigame = Minigame.getInstance();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        if (Minigame.getInstance().getCosmeticsManager() != null) {
            QueryResult cosmeticResult = connection.update()
                    .table(minigame.getMinigameTable().getTableName())
                    .set("Cosmetics", CosmeticsStorage.toJSON(getGamePlayer()).toString())
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .execute();
            if (!cosmeticResult.isSuccessful()) {
                Logger.log("Something went wrong when saving cosmetics data! The following message is for Developers: ", Logger.LogType.ERROR);
                Logger.log(cosmeticResult.getRejectMessage(), Logger.LogType.ERROR);
                getGamePlayer().getOnlinePlayer().sendMessage("§cAn error occurred while saving cosmetics data. Sorry for the inconvenience.");
            }
        }
    }



    private void loadCosmetics(){
        Minigame minigame = Minigame.getInstance();
        SQLDatabaseConnection connection = minigame.getDatabase().getConnection();

        if (Minigame.getInstance().getCosmeticsManager() == null){
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

                    for (CosmeticsCategory category : Minigame.getInstance().getCosmeticsManager().getCategories()) {
                        if (!jsonObject.getJSONArray("purchased").isEmpty())
                            purchasedCosmetics = CosmeticsStorage.parseJsonArrayToList(jsonObject.getJSONArray("purchased"));


                        if (!jsonObject.getJSONArray("selected").isEmpty()) {
                            List<Cosmetic> cosmetics = CosmeticsStorage.parseJsonArrayToList(jsonObject.getJSONArray("selected")).stream().filter(c -> Minigame.getInstance().getCosmeticsManager().getCategory(c) != null && Minigame.getInstance().getCosmeticsManager().getCategory(c).equals(category)).toList();
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

        if (Minigame.getInstance().getCosmeticsManager().getCategory(cosmetic) != null) {
            if (selectedCosmetics == null)
                selectedCosmetics = new HashMap<>();

            if (!selectedCosmetics.containsValue(cosmetic))
                selectedCosmetics.put(Minigame.getInstance().getCosmeticsManager().getCategory(cosmetic), cosmetic);
        }
    }

    public Cosmetic getSelectedCosmetic(CosmeticsCategory category){
        if (selectedCosmetics == null)
            return null;

        return selectedCosmetics.get(category);
    }

    public void purchaseCosmetic(Cosmetic cosmetic){
        if (purchasedCosmetics == null)
            purchasedCosmetics = new ArrayList<>();

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
