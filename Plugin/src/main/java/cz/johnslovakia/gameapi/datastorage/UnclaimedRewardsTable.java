package cz.johnslovakia.gameapi.datastorage;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.messages.Language;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.UnclaimedReward;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.Row;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class UnclaimedRewardsTable {

    public static final String TABLE_NAME = "unclaimed_rewards";


    public static void addUnclaimedReward(UnclaimedReward unclaimedReward, String dataJSON){
        if (Minigame.getInstance().getDatabase() == null){
            Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
            return;
        }
        SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
        if (connection == null){
            return;
        }

        QueryResult result = connection.insert()
                .into(TABLE_NAME, "Nickname", "type", "reward_json", "data_json", "created_at") //Timestamp.from(unclaimedReward.getCreatedAt().toInstant(ZoneOffset.UTC)
                .values(unclaimedReward.getGamePlayer().getOnlinePlayer().getName(), unclaimedReward.getType().name(), unclaimedReward.getRewardJson(), dataJSON, unclaimedReward.getCreatedAt().toString())
                .execute();

        if (!result.isSuccessful()){
            unclaimedReward.claim();
            Logger.log("Something went wrong when saving player's unclaimed reward to database! The following message is for Developers: ", Logger.LogType.ERROR);
            Logger.log(result.getRejectMessage(), Logger.LogType.INFO);
        }
    }

    public static void removeUnclaimedReward(UnclaimedReward unclaimedReward){
        GamePlayer gamePlayer = unclaimedReward.getGamePlayer();

        if (Minigame.getInstance().getDatabase() == null){
            Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
            return;
        }
        SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
        if (connection == null){
            return;
        }


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedCreatedAt = unclaimedReward.getCreatedAt().format(formatter);

        QueryResult result = connection.delete()
                .from(TABLE_NAME)
                .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                .and().isEqual("type", unclaimedReward.getType().name())
                .and().isEqual("created_at", formattedCreatedAt)
                .and().isEqual("data_json", unclaimedReward.getData().toString())
                .execute();

        if (!result.isSuccessful()){
            Logger.log("Something went wrong when deleting player's unclaimed reward! The following message is for Developers: ", Logger.LogType.ERROR);
            Logger.log(result.getRejectMessage(), Logger.LogType.INFO);
        }
    }

    public static void createTable() {
        String rows = "id INT AUTO_INCREMENT PRIMARY KEY," +
                "Nickname VARCHAR(36) NOT NULL," +
                "type VARCHAR(32) NOT NULL," +
                "reward_json TEXT NOT NULL," +
                "data_json TEXT," +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP";

        SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
        if (connection == null){
            return;
        }
        QueryResult result = connection.exec(() ->
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
                        + rows + ");");

        if (!result.isSuccessful()) {
            Logger.log("Failed to create " + TABLE_NAME + " table!", Logger.LogType.ERROR);
            Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
        }
    }
}
