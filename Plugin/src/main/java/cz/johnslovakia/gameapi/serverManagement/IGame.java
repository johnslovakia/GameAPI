package cz.johnslovakia.gameapi.serverManagement;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.Utils;

import lombok.Getter;
import me.zort.sqllib.api.data.QueryResult;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

@Getter
public class IGame {

    @Getter
    public static List<IGame> servers = new ArrayList<>();

    private final String minigameName, name;

    public IGame(String minigame, String name) {
        this.minigameName = minigame;
        this.name = name;
    }

    public boolean isOpen(){
        GameData gameData = GameAPI.getInstance().getMinigame().getDataManager().getGameDataByGame(this);

        if (GameAPI.getInstance().getMinigame().getName().toLowerCase().contains("minianni")){
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
        Minigame minigame = GameAPI.getInstance().getMinigame();

        String server = getBungeecordServerName();
        new BukkitRunnable(){
            @Override
            public void run() {
                if (isMultiArena()) {
                    if (GameAPI.getInstance().getMinigame().useRedisForServerData()) {
                        String key = "player:" + gamePlayer.getOnlinePlayer().getName() + ":game";
                        minigame.getServerDataRedis().getPool().getResource().setex(key, 60, getArenaID());
                    } else {
                        QueryResult perksResult = minigame.getDatabase().getConnection().update()
                                .table(minigame.getMinigameTable().getTableName())
                                .set("game", getArenaID())
                                .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                                .execute();
                        if (!perksResult.isSuccessful()) {
                            Logger.log(perksResult.getRejectMessage(), Logger.LogType.ERROR);
                        }
                    }
                    Utils.send(gamePlayer.getOnlinePlayer(), server);
                }
            }
        }.runTaskAsynchronously(GameAPI.getInstance());
    }





    public static  void addGame(IGame server){
        if (!servers.contains(server)) servers.add(server);
    }

}
