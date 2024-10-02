package cz.johnslovakia.gameapi.users;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.datastorage.CosmeticsStorage;
import cz.johnslovakia.gameapi.datastorage.KitsStorage;
import cz.johnslovakia.gameapi.datastorage.PlayerTable;
import cz.johnslovakia.gameapi.datastorage.QuestsStorage;
import cz.johnslovakia.gameapi.economy.Economy;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
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
import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.Row;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.*;

@Getter @Setter
public class PlayerData {

    private GamePlayer gamePlayer;
    private PlayerTable playerTable;

    private Game game;
    private GameTeam team;
    private Kit kit;
    private String prefix;
    private Language language;

    private List<GameMap> votesForMaps = new ArrayList<>();

    private Map<CosmeticsCategory, Cosmetic> selectedCosmetics = new HashMap<>();
    private List<Cosmetic> purchasedCosmetics = new ArrayList<>();

    private List<Kit> purchasedKitsThisGame = new ArrayList<>();
    private Map<Kit, Inventory> kitInventories = new HashMap<>();
    private Kit defaultKit;

    private List<PlayerQuestData> quests = new ArrayList<>();

    private Map<Perk, Integer> perksLevel = new HashMap<>();

    public PlayerData(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
        this.playerTable = new PlayerTable(gamePlayer);

        try {
            Optional<Row> result = GameAPI.getInstance().getMinigame().getDatabase().select()
                    .from(PlayerTable.TABLE_NAME)
                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                    .obtainOne();

            String lang = result.get().getString("Language");
            if (lang == null || Language.getLanguage(lang) == null) {
                language = Language.getDefaultLanguage();
            } else {
                language = Language.getLanguage(lang);
            }
        } catch (Exception e) {
            language = Language.getDefaultLanguage();
            e.printStackTrace();
        }

        new BukkitRunnable(){
            @Override
            public void run() {
                loadKits();
                loadCosmetics();
                loadPerks();
                loadQuests();

                GameAPI.getInstance().getQuestManager().check(gamePlayer);
            }
        }.runTaskAsynchronously(GameAPI.getInstance());
    }

    public void addQuestProgress(Quest quest){
        for (PlayerQuestData questData : quests){
            if (questData.getQuest().equals(quest)){
                questData.increaseProgress();
                break;
            }
        }
    }

    public PlayerQuestData getQuestData(Quest quest){
        for (PlayerQuestData questData : quests){
            if (questData.getQuest().equals(quest)){
                return questData;
            }
        }
        return null;
    }

    private void loadQuests(){
        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase();

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
                JSONObject jsonObject = new JSONObject(jsonString);

                JSONArray questsArray = jsonObject.getJSONArray("quests");

                for (int i = 0; i < questsArray.length(); i++) {
                    JSONObject questObject = questsArray.getJSONObject(i);
                    String name = questObject.getString("name");
                    QuestType type = QuestType.valueOf(questObject.getString("type").toUpperCase());
                    PlayerQuestData.Status status = PlayerQuestData.Status.valueOf(questObject.getString("status").toUpperCase());  // "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
                    int progress = questObject.getInt("progress");
                    String completionDateString = questObject.optString("completion_date", null);
                    LocalDate completionDate = null;
                    if (completionDateString != null) {
                        completionDate = LocalDate.parse(questObject.optString("completion_date", null));
                    }

                    Quest quest = GameAPI.getInstance().getQuestManager().getQuest(type, name);

                    PlayerQuestData questData = new PlayerQuestData(quest, gamePlayer);
                    if (status.equals(PlayerQuestData.Status.COMPLETED)) {
                        questData = new PlayerQuestData(quest, gamePlayer, completionDate);
                    }else if (status.equals(PlayerQuestData.Status.IN_PROGRESS)){
                        questData = new PlayerQuestData(quest, gamePlayer, progress);
                    }

                    quests.add(questData);
                }
            } catch (Exception exception) {
                Logger.log("I can't get quests data for player " + gamePlayer.getOnlinePlayer().getName() + ". (2)", Logger.LogType.ERROR);
                gamePlayer.getOnlinePlayer().sendMessage("Can't get your quests data. Sorry for the inconvenience. (2)");
            }
        }
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
        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase();

        Optional<Row> result = connection.select()
                .from(minigame.getMinigameTable().getTableName())
                .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                .obtainOne();
        if (!result.isPresent()) {
            Logger.log("I can't get perks data for player " + gamePlayer.getOnlinePlayer().getName() + ". (1)", Logger.LogType.ERROR);
            gamePlayer.getOnlinePlayer().sendMessage("Can't get your perks data. Sorry for the inconvenience. (1)");
        }else {
            try{
                String jsonString = result.get().getString("Perks");
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
            } catch (Exception exception) {
                Logger.log("I can't get perks data for player " + gamePlayer.getOnlinePlayer().getName() + ". (2)", Logger.LogType.ERROR);
                gamePlayer.getOnlinePlayer().sendMessage("Can't get your perks data. Sorry for the inconvenience. (2)");
            }
        }
    }

    public void setKitInventory(Kit kit, Inventory inventory){
        kitInventories.put(kit, inventory);
    }

    private void loadKits(){
        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase();

        Optional<Row> result = connection.select()
                .from(minigame.getMinigameTable().getTableName())
                .where().isEqual("Nickname", getGamePlayer().getOnlinePlayer().getName())
                .obtainOne();
        if (!result.isPresent()) {
            Logger.log("I can't get kits data for player " + gamePlayer.getOnlinePlayer().getName() + ". (1)", Logger.LogType.ERROR);
            gamePlayer.getOnlinePlayer().sendMessage("Can't get your kits data. Sorry for the inconvenience. (1)");
        }else {
            try{
                String rDKit = result.get().getString("DefaultKit");
                if (rDKit != null) {
                    Kit dKit = GameAPI.getInstance().getKitManager().getKit(result.get().getString("DefaultKit"));
                    if (dKit != null) {
                        this.defaultKit = dKit;
                    }
                }


                String jsonString = result.get().getString("KitInventories");
                JSONObject jsonObject = new JSONObject(jsonString);

                JSONArray inventoriesArray = jsonObject.getJSONArray("inventories");

                for (int i = 0; i < inventoriesArray.length(); i++) {
                    JSONObject kitObject = inventoriesArray.getJSONObject(i);
                    String name = kitObject.getString("name");
                    String inventory = kitObject.getString("inventory");
                    Kit k = GameAPI.getInstance().getKitManager().getKit(name);

                    if (k != null) {
                        kitInventories.put(k, BukkitSerialization.fromBase64(inventory));
                    }
                }
            } catch (Exception exception) {
                Logger.log("I can't get kits data for player " + gamePlayer.getOnlinePlayer().getName() + ". (2)", Logger.LogType.ERROR);
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
        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase();

        QueryResult cosmeticResult = connection.insert()
                .into(minigame.getMinigameTable().getTableName(), "Cosmetics")
                .values(CosmeticsStorage.toJSON(getGamePlayer()))
                .execute();
        if (!cosmeticResult.isSuccessful()) {
            Logger.log("Something went wrong when saving cosmetics data! The following message is for Developers: ", Logger.LogType.ERROR);
            Logger.log(cosmeticResult.getRejectMessage(), Logger.LogType.ERROR);
        }


        QueryResult questsResult = connection.insert()
                .into(minigame.getMinigameTable().getTableName(), "Quests")
                .values(QuestsStorage.toJSON(getGamePlayer()))
                .execute();
        if (!questsResult.isSuccessful()) {
            Logger.log("Something went wrong when saving quests data! The following message is for Developers: ", Logger.LogType.ERROR);
            Logger.log(questsResult.getRejectMessage(), Logger.LogType.ERROR);
        }


        QueryResult kitInventoriesResult = connection.insert()
                .into(minigame.getMinigameTable().getTableName(), "KitInventories")
                .values(KitsStorage.inventoriesToJSON(getGamePlayer()))
                .execute();
        if (!kitInventoriesResult.isSuccessful()) {
            Logger.log("Something went wrong when saving kits data! The following message is for Developers: ", Logger.LogType.ERROR);
            Logger.log(cosmeticResult.getRejectMessage(), Logger.LogType.ERROR);
        }
    }

    private void loadCosmetics(){
        Minigame minigame = GameAPI.getInstance().getMinigame();
        SQLDatabaseConnection connection = minigame.getDatabase();

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
                JSONObject jsonObject = new JSONObject(jsonString);

                for (CosmeticsCategory category : GameAPI.getInstance().getCosmeticsManager().getCategories()){
                    Cosmetic cosmetic = CosmeticsStorage.parseJsonArrayToList(jsonObject.getJSONArray("selected")).stream().filter(c -> GameAPI.getInstance().getCosmeticsManager().getCategory(c).equals(category)).toList().get(0);
                    if (cosmetic != null) {
                        selectedCosmetics.put(category, cosmetic);
                    }
                }
                purchasedCosmetics = CosmeticsStorage.parseJsonArrayToList(jsonObject.getJSONArray("purchased"));
            } catch (Exception exception) {
                Logger.log("I can't get cosmetics data for player " + gamePlayer.getOnlinePlayer().getName() + ". (2)", Logger.LogType.ERROR);
                gamePlayer.getOnlinePlayer().sendMessage("Can't get your cosmetics data. Sorry for the inconvenience. (2)");
            }
        }
    }

    public void selectCosmetic(Cosmetic cosmetic){
        if (GameAPI.getInstance().getCosmeticsManager().getCategory(cosmetic) != null) {
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


    public void deposit(Economy economy, int amount) {
        if (economy != null) {
            economy.getEconomyInterface().deposit(gamePlayer, amount);
        }
    }

    public void withdraw(Economy economy, int amount) {
        if (economy != null) {
            economy.getEconomyInterface().withdraw(gamePlayer, amount);
        }
    }

    public int getBalance(Economy economy) {
        if (economy != null) {
            return economy.getEconomyInterface().getBalance(gamePlayer);
        }
        return 0;
    }
}
