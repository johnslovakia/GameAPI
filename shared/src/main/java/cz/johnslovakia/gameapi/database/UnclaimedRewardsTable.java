package cz.johnslovakia.gameapi.database;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.rewards.unclaimed.UnclaimedReward;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.Logger;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;

import java.time.format.DateTimeFormatter;

public class UnclaimedRewardsTable {

    public static final String TABLE_NAME = "unclaimed_rewards";

    public static void addUnclaimedReward(UnclaimedReward unclaimedReward, String dataJSON){
        if (Shared.getInstance().getDatabase() == null){
            Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
            return;
        }
        SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();
        if (connection == null){
            return;
        }

        QueryResult result = connection.insert()
                .into(TABLE_NAME, "Nickname", "type", "reward_json", "data_json", "created_at") //Timestamp.from(unclaimedReward.getCreatedAt().toInstant(ZoneOffset.UTC)
                .values(unclaimedReward.getPlayerIdentity().getOnlinePlayer().getName(), unclaimedReward.getType().name(), unclaimedReward.getRewardJson(), dataJSON, unclaimedReward.getCreatedAt().toString())
                .execute();

        if (!result.isSuccessful()){
            unclaimedReward.claim();
            Logger.log("Something went wrong when saving player's unclaimed reward to database! The following message is for Developers: ", Logger.LogType.ERROR);
            Logger.log(result.getRejectMessage(), Logger.LogType.INFO);
        }
    }

    public static void removeUnclaimedReward(UnclaimedReward unclaimedReward){
        PlayerIdentity gamePlayer = unclaimedReward.getPlayerIdentity();

        if (Shared.getInstance().getDatabase() == null){
            Logger.log("You don't have the database set up in the config.yml!", Logger.LogType.ERROR);
            return;
        }
        SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();
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

        SQLDatabaseConnection connection = Shared.getInstance().getDatabase().getConnection();
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
