package cz.johnslovakia.gameapi.users.stats;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.utils.ConfigAPI;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.actions.Action;
import eu.decentsoftware.holograms.api.actions.ActionType;
import eu.decentsoftware.holograms.api.actions.ClickType;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramPage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatsHolograms {

    public static void createPlayerStatisticsHologram(StatsManager manager, Location location, Player player){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        List<String> lines = new ArrayList<>();
        //lines.add("§2§lYour Lifetime stats in " + GameAPI.getMinigameName());
        lines.add(MessageManager.get(gamePlayer, "hologram.lifetime_stats").replace("%minigame_name%", GameAPI.getInstance().getMinigame().getMinigameName()).getTranslated());
        for (Stat stat : manager.getStats()){
            PlayerStat playerStat = stat.getPlayerStat(gamePlayer);
            if (MessageManager.existMessage("hologram.lifetime_stats.score_line")){
                lines.add(MessageManager.get(player, "hologram.lifetime_stats.score_line").replace("%stat_name%", stat.getTranslated(gamePlayer)).replace("%value%", "" + playerStat.getStatScore()).getTranslated());
            }else {
                lines.add("§f" + stat.getTranslated(gamePlayer) + ": §a" + playerStat.getStatScore());
            }
            //DHAPI.addHologramLine(hologram, "§f" + stat.getName() + ": §a" + playerStat.getStatScore());
        }


        if (DHAPI.getHologram("stats_" + player.getName()) != null){
            DHAPI.setHologramLines(DHAPI.getHologram("stats_" + player.getName()), lines);
            return;
        }

        Hologram hologram = DHAPI.createHologram("stats_" + player.getName(), location.add(0, 3, 0));
        hologram.setDefaultVisibleState(false);
        hologram.setShowPlayer(player);

        DHAPI.setHologramLines(hologram, lines);
    }

    public static void createTOPStatisticsHologram(StatsManager manager, Location location, Player player){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);


        /*List<String> lines = new ArrayList<>();
        lines.add(MessageManager.get(gamePlayer, "hologram.top_stats").replace("%minigame_name%", GameAPI.getMinigameName()).getTranslated());
        for (Stat stat : getStats()){
            PlayerStat playerStat = stat.getPlayerStat(gamePlayer);
            lines.add("§f" + stat.getName() + ": §a" + playerStat.getStatScore());
            //DHAPI.addHologramLine(hologram, "§f" + stat.getName() + ": §a" + playerStat.getStatScore());
        }
*/

        if (DHAPI.getHologram("topStats_" + player.getName()) != null){
            return;
        }


        boolean b = false;
        Hologram hologram = DHAPI.createHologram("topStats_" + player.getName(), location.add(0, 3, 0));
        hologram.setDefaultVisibleState(false);
        hologram.setShowPlayer(player);


        for (Stat stat : manager.getStats()){
            List<String> lines = new ArrayList<>();
            lines.add(MessageManager.get(gamePlayer, "hologram.top_stats").replace("%stat%", stat.getTranslated(gamePlayer)).getTranslated());

            List<Map.Entry<String, Integer>> topMap = GameAPI.getInstance().getStatsManager().getStatsTable().topStats(stat.getName(), 10).entrySet()
                    .stream()
                    .sorted((o1, o2) -> -Integer.compare(o1.getValue(), o2.getValue()))
                    .limit(10)
                    .toList();

            for (int i = 0; i <= 9; i++){
                if (i < topMap.size()) {
                    Map.Entry<String, Integer> map = topMap.get(i);

                    if (MessageManager.existMessage("hologram.top_stats.score_line")){
                        lines.add(MessageManager.get(player, "hologram.top_stats.score_line")
                                .replace("%stat_name%", stat.getTranslated(gamePlayer))
                                .replace("%value%", "" + map.getValue())
                                .replace("%player%", map.getKey())
                                .replace("%position%", "" + (i + 1)).getTranslated());
                    }else {
                        lines.add("§f" + (i + 1) + ". §a" + map.getKey() + " §8- §a" + map.getValue() + " " + stat.getTranslated(gamePlayer));
                    }
                }else{
                    lines.add("§f" + (i + 1) + ". §8-");
                }
            }

            lines.add("");
            lines.add(MessageManager.get(gamePlayer, "hologram.next_page").getTranslated());

            if (!b){
                DHAPI.setHologramLines(hologram, lines);
                b = true;
            }else {
                HologramPage page = DHAPI.addHologramPage(hologram, lines);
                page.addAction(ClickType.RIGHT, new Action(ActionType.NEXT_PAGE, ""));
            }
        }
    }

    public static void remove(Player player){
        ConfigAPI config = new ConfigAPI(GameAPI.getInstance().getMinigameDataFolder().toString(), "config.yml", GameAPI.getInstance());
        if (config.getLocation("statsHologram") != null) {
            if (DHAPI.getHologram("stats_" + player.getName()) != null) {
                DHAPI.removeHologram("stats_" + player.getName());
            }
        }
        if (config.getLocation("topStatsHologram") != null) {
            if (DHAPI.getHologram("topStats_" + player.getName()) != null) {
                DHAPI.removeHologram("topStats_" + player.getName());
            }
        }
    }
}
