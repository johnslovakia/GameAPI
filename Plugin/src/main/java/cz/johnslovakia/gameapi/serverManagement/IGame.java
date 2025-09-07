package cz.johnslovakia.gameapi.serverManagement;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.Utils;

import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.api.data.QueryResult;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

@Getter
public class IGame {

    private final IMinigame minigame;
    private final String name;

    @Setter
    private GameData gameData;

    public IGame(IMinigame minigame, String name) {
        this.minigame = minigame;
        this.name = name;
    }

    public boolean isOpen(){
        GameData gameData = minigame.getGameDataByGame(this);

        if (Minigame.getInstance().getName().toLowerCase().contains("minianni")){
            if (gameData.getGameState().equals(GameState.INGAME)){
                if (gameData.getJsonObject().get("Phase") != null){
                    int phase = gameData.getJsonObject().get("Phase").getAsInt();
                    if (phase == 1
                            || phase == 2){
                        return true;
                    }
                }
            }
        }

        return gameData.getGameState().equals(GameState.WAITING) || gameData.getGameState().equals(GameState.STARTING);
    }

    public String getBungeecordServerName(){
        String server = name;
        if (!Character.isDigit(name.charAt(name.length()-1))){
            server = name.substring(0, name.length() - 1);
        }

        return server;
    }

    public boolean isMultiArena(){
        return !Character.isDigit(name.charAt(name.length()-1));
    }

    public String getArenaID(){
        return String.valueOf(name.charAt(name.length() - 1));
    }

    public void sendPlayerToServer(GamePlayer gamePlayer){
        String server = getBungeecordServerName();
        new BukkitRunnable(){
            @Override
            public void run() {
                if (isMultiArena()) {
                    try {
                        if (DataManager.getInstance().useRedisForServerData()) {
                            String key = "player:" + gamePlayer.getOnlinePlayer().getName() + ":game";
                            minigame.getDataManager().getServerDataRedis().getPool().getResource().setex(key, 60, getArenaID());
                        } else {
                            //TODO: připojování na arénu přes MySQL
                            /*QueryResult result = minigame.getDataManager().getServerDataMySQL().getConnection().update()
                                    .table(minigame.getMinigameTable().getTableName())
                                    .set("game", getArenaID())
                                    .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                    .execute();
                            if (!result.isSuccessful()) {
                                Logger.log(result.getRejectMessage(), Logger.LogType.ERROR);
                            }*/
                        }
                        Utils.send(gamePlayer.getOnlinePlayer(), server);
                    }catch (Exception exception){
                        MessageManager.get(gamePlayer, "chat.something_wrong.new_game")
                                .send();
                        exception.printStackTrace();
                    }
                }
            }
        }.runTaskAsynchronously(Minigame.getInstance().getPlugin());
    }
}
