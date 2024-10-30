package cz.johnslovakia.gameapi.game.map;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameManager;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AreaManager {

    public static AreaSettings getLimitedSettings(){
        AreaSettings settings = new AreaSettings();

        settings.setPriority(100);
        settings.setAllowChestAccess(false);
        settings.setAllowFlintAndSteel(false);
        settings.setCanBreakAll(false);
        settings.setCanPlaceAll(false);
        settings.setAllowItemDrop(false);
        settings.setAllowItemFrameDamage(false);
        settings.setAllowPaintingDamage(false);
        settings.setAllowMobDamage(false);
        settings.setAllowEnderpearlFallDamage(false);
        settings.setAllowFallDamage(false);
        settings.setAllowPlayerSleep(false);
        settings.setAllowFoodLevelChange(false);
        settings.setAllowDurabilityChange(false);
        settings.setAllowInventoryChange(false);
        settings.setAllowItemPicking(false);
        settings.setAllowPlayerInteract(false);

        return settings;
    }


    /**
     * Get a list of active settings for a position.
     * If two ArenaSetting's priorities conflict, the first one that the computer finds is chosen.
     *
     * @return AreaSettings object for the location, or null if no settings apply.
     */
    public static AreaSettings getActiveSettings(Location location) {
        int topPriority = -1;
        AreaSettings settings = null;

        for (AreaSettings setting : getAreaSettingsByLocation(location)) {
            if (setting.getPriority() > topPriority) {
                settings = setting;
                topPriority = setting.getPriority();
            }
        }

        return settings;
    }


    /**
     * Get a list of active settings for a player.
     * If two ArenaSetting's priorities conflict, the first one that the computer finds is chosen.
     *
     * @param gamePlayer GamePlayer
     * @return AreaSettings object with active settings, or null if the player is not in a game.
     */
    public static AreaSettings getActiveSettings(GamePlayer gamePlayer) {
        Game game = gamePlayer.getPlayerData().getGame();

        if (gamePlayer.isLimited()) return getLimitedSettings();
        if (game == null) return null;
        if (game.getState() != GameState.INGAME) return getLimitedSettings();
        if (game.getPlayingMap() == null) return null;
        if (gamePlayer.isSpectator()) return getLimitedSettings();
        if (gamePlayer.getAreas().isEmpty()) return game.getPlayingMap().getSettings();

        AreaSettings settings = null;
        int topPriority = -1;

        for (Area area : gamePlayer.getAreas()) {
            if (area.getSettings().getPriority() > topPriority) {
                settings = area.getSettings();
                topPriority = area.getSettings().getPriority();
            }
        }

        return (settings == null ? game.getPlayingMap().getSettings() : settings);
    }


    /**
     * Get a list of AreaSettings that apply to a location. This is cross-game!
     *
     * @param location Bukkit Location.
     * @return List<Area>
     */
    public static List<AreaSettings> getAreaSettingsByLocation(Location location) {
        List<AreaSettings> settings = new ArrayList<AreaSettings>();

        for(Game game : GameManager.getGames()) {
            if (game.getPlayingMap() == null) {
                continue;
            }
            if (game.getPlayingMap().getWorld().getName().equals(Objects.requireNonNull(location.getWorld()).getName())) {
                settings.add(game.getPlayingMap().getSettings());
            }
            for (Area area : game.getPlayingMap().getAreas()) {
                if (area.isInArea(location)){
                    settings.add(area.getSettings());
                }
            }
        }
        return settings;
    }
}
