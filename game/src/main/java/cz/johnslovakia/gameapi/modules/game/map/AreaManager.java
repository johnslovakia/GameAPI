package cz.johnslovakia.gameapi.modules.game.map;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameService;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AreaManager {

    public static AreaSettings getLimitedSettings(GamePlayer gamePlayer){
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
        if (!gamePlayer.getGame().isPreparation()) {
            settings.setAllowInventoryChange(false);
        }
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
        GameInstance game = gamePlayer.getGame();

        if (gamePlayer.getGameSession() == null) return getLimitedSettings(gamePlayer);
        if (gamePlayer.getGameSession().isLimited()) return getLimitedSettings(gamePlayer);
        if (game == null) return null;
        if (game.getState() != GameState.INGAME) return getLimitedSettings(gamePlayer);
        if (game.getCurrentMap() == null) return null;
        if (gamePlayer.isSpectator()) return getLimitedSettings(gamePlayer);
        if (gamePlayer.getAreas() == null) return game.getCurrentMap().getSettings();
        if (gamePlayer.getAreas().isEmpty()) return game.getCurrentMap().getSettings();

        AreaSettings settings = null;
        int topPriority = -1;

        for (Area area : gamePlayer.getAreas()) {
            if (area.getSettings().getPriority() > topPriority) {
                settings = area.getSettings();
                topPriority = area.getSettings().getPriority();
            }
        }

        return (settings == null ? game.getCurrentMap().getSettings() : settings);
    }


    /**
     * Get a list of AreaSettings that apply to a location. This is cross-game!
     *
     * @param location Bukkit Location.
     * @return List<Area>
     */
    public static List<AreaSettings> getAreaSettingsByLocation(Location location) {
        List<AreaSettings> settings = new ArrayList<AreaSettings>();

        for(GameInstance game : ModuleManager.getModule(GameService.class).getGames().values()) {
            if (game.getState() != GameState.INGAME || game.getCurrentMap() == null) continue;
            if (!game.getCurrentMap().getWorld().equals(location.getWorld())) continue;

            if (game.getCurrentMap().getWorld().getName().equals(Objects.requireNonNull(location.getWorld()).getName())) {
                settings.add(game.getCurrentMap().getSettings());
            }
            for (Area area : game.getCurrentMap().getAreas()) {
                if (area.isInArea(location)){
                    settings.add(area.getSettings());
                }
            }
        }
        return settings;
    }
}
