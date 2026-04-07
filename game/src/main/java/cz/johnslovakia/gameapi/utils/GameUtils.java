package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.lobby.LobbyLocation;
import cz.johnslovakia.gameapi.modules.game.map.GameMap;
import cz.johnslovakia.gameapi.modules.game.map.MapLocation;
import cz.johnslovakia.gameapi.modules.game.session.GameSessionModule;
import cz.johnslovakia.gameapi.modules.game.session.PlayerGameSession;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class GameUtils {

    public static MapLocation getMapLocation(FileConfiguration fileConfig, GameMap gameMap, String id, String path){
        return getMapLocation(fileConfig, gameMap, id, path, false);
    }

    public static MapLocation getMapLocation(FileConfiguration fileConfig, GameMap gameMap, String id, String path, boolean yaw_and_pitch){
        if (fileConfig.get(path) == null) {
            Logger.log("getMapLocation: Path '" + path + "' is null!", Logger.LogType.ERROR);
            return null;
        }
        return GameUtils.getMapLocationFromString(gameMap, id, fileConfig.getString(path), yaw_and_pitch);
    }

    public static LobbyLocation getLobbyLocation(FileConfiguration fileConfig, GameInstance game, String path){
        if (fileConfig.get(path) == null) {
            Logger.log("getLobbyLocation: Path '" + path + "' is null!", Logger.LogType.ERROR);
            return null;
        }

        final String[] parts = fileConfig.getString(path).split(";");
        final String world = parts[0];
        final double x = Double.parseDouble(parts[1]);
        final double y = Double.parseDouble(parts[2]);
        final double z = Double.parseDouble(parts[3]);
        final float yaw =  Float.parseFloat(parts[4]);
        final float pitch =  Float.parseFloat(parts[5]);

        return new LobbyLocation(game, world, x, y, z, yaw, pitch);
    }



    public static Location getNonRespawnLocation(GameInstance game){
        GameMap playingMap = game.getCurrentMap();
        MapLocation spectatorSpawn = playingMap.getSpectatorSpawn();

        if (spectatorSpawn == null){
            if (game.getCurrentMap().getMainArea() != null) {
                Location center = playingMap.getMainArea().getCenter().add(0, 15, 0);
                while (center.getBlock().getType().equals(Material.AIR)) {
                    center.add(0, 1, 0);
                }
                return center;
            }else{
                if (game.getPlayers().get(0) != null){
                    return game.getPlayers().get(0).getOnlinePlayer().getLocation();
                }else{
                    return new Location(game.getCurrentMap().getWorld(), 0, 90, 0);
                }
            }
        }
        return spectatorSpawn.getLocation();
    }

    public static void hideAndShowPlayers(GameInstance game, Player player){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        if (gamePlayer == null) return;
        for (Player serverPlayer : Bukkit.getOnlinePlayers()){
            player.hidePlayer(Core.getInstance().getPlugin(), serverPlayer);
            serverPlayer.hidePlayer(Core.getInstance().getPlugin(), player);

            for (GamePlayer targetGamePlayer : game.getParticipants()){
                Player target = targetGamePlayer.getOnlinePlayer();
                if (!gamePlayer.isSpectator()) {
                    target.showPlayer(Core.getInstance().getPlugin(), player);
                }
                player.showPlayer(Core.getInstance().getPlugin(), target);
            }
        }
    }

    static public MapLocation getMapLocationFromString(GameMap gameMap, String id, String s, boolean yaw_and_pitch) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        final String[] parts = s.split(";");
        if (parts.length == 5) {
            final double x = Double.parseDouble(parts[0]);
            final double y = Double.parseDouble(parts[1]);
            final double z = Double.parseDouble(parts[2]);
            final float yaw =  Float.parseFloat(parts[3]);
            final float pitch =  Float.parseFloat(parts[4]);
            if (yaw_and_pitch) {
                return new MapLocation(gameMap, id, x, y, z, yaw, pitch);
            }else{
                return new MapLocation(gameMap, id, x, y, z);
            }
        }else if (parts.length == 6) {
            final String world = parts[0];
            final double x = Double.parseDouble(parts[1]);
            final double y = Double.parseDouble(parts[2]);
            final double z = Double.parseDouble(parts[3]);
            final float yaw =  Float.parseFloat(parts[4]);
            final float pitch =  Float.parseFloat(parts[5]);
            if (yaw_and_pitch) {
                return new MapLocation(gameMap, id, x, y, z, yaw, pitch);
            }else{
                return new MapLocation(gameMap, id, x, y, z);
            }
        }
        Logger.log("getMapLocationFromString Error: " + parts.length + " " + id + " " + s + " " + gameMap.getName(), Logger.LogType.ERROR);
        return null;
    }

    public static void sendToLobby(Player player){
        sendToLobby(player, true);
    }

    public static void sendToLobby(Player player, boolean message){
        ConfigAPI config = new ConfigAPI(GameAPI.getInstance().getMinigameDataFolder().toString(), "config.yml", Minigame.getInstance().getPlugin());
        List<String> lobbies = config.getConfig().getStringList("lobby_servers");

        if (message) ModuleManager.getModule(MessageModule.class).get(player, "chat.sending_to_lobby").send();

        Collections.shuffle(lobbies);
        Utils.sendToServer(player, (lobbies.isEmpty() ? "Lobby" : lobbies.get(0)));

        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> player.kick(ModuleManager.getModule(MessageModule.class).get(player, "kick.offline_server").getTranslated()), 50L);
    }

    public static void colorizeArmor(GamePlayer gamePlayer) {
        Player player = gamePlayer.getOnlinePlayer();
        PlayerInventory inv = player.getInventory();
        GameTeam team = gamePlayer.getGameSession().getTeam();

        if (team == null){
            return;
        }

        for (ItemStack item : inv.getArmorContents()) {
            if (item == null || !(item.getItemMeta() instanceof LeatherArmorMeta)) {
                continue;
            }
            LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
            Color color = team.getColor();
            meta.setColor(color);
            item.setItemMeta(meta);
        }
    }

    public static Map<GamePlayer, Integer> getTopPlayers(GameInstance game, String scoreName, int maxRank, int hardCap) {
        List<PlayerGameSession> sessions = game.getModule(GameSessionModule.class).getPlayerSessions();

        List<PlayerGameSession> sorted = sessions.stream()
                .filter(s -> s.getScore(scoreName) > 0)
                .sorted((s1, s2) -> Integer.compare(s2.getScore(scoreName), s1.getScore(scoreName)))
                .toList();

        Map<GamePlayer, Integer> result = new LinkedHashMap<>();
        int rank = 1;

        for (int i = 0; i < sorted.size(); i++) {
            PlayerGameSession session = sorted.get(i);

            if (i > 0 && session.getScore(scoreName) != sorted.get(i - 1).getScore(scoreName)) {
                rank = i + 1;
            }

            if (rank > maxRank) break;

            result.put((GamePlayer) session.getPlayerIdentity(), rank);

            if (result.size() >= hardCap) break;
        }

        return result;
    }

    public static boolean isPlayerDamager(EntityDamageByEntityEvent e){
        return getDamager(e) != null;
    }

    public static GamePlayer getDamager(EntityDamageByEntityEvent e){
        if(e.getDamager() instanceof Player){
            return PlayerManager.getGamePlayer(((Player) e.getDamager()));
        }else if(e.getDamager() instanceof Projectile){
            Projectile a = (Projectile) e.getDamager();
            if(a.getShooter() != null){
                if(a.getShooter() instanceof Player){
                    return PlayerManager.getGamePlayer((Player)a.getShooter());
                }
            }
        }
        return null;
    }

    public static double getDamageForPlayer(Player player, double damage) {
        if (PlayerManager.getGamePlayer(player).isSpectator()){
            return 0.0;
        }

        double points = Objects.requireNonNull(
                player.getAttribute(Attribute.ARMOR)
        ).getValue();
        double toughness = Objects.requireNonNull(
                player.getAttribute(Attribute.ARMOR_TOUGHNESS)
        ).getValue();


        PotionEffect effect = player.getPotionEffect(PotionEffectType.RESISTANCE);
        int resistance = effect == null ? 0 : effect.getAmplifier();
        int epf = Utils.getEPF(player.getInventory());

       return Utils.calculateDamageApplied(damage, points, toughness, resistance, epf);
    }

    public static void setTeamNameTag(Player player, String id, ChatColor chatColor) {
        for (Player each : Bukkit.getOnlinePlayers()) {
            Scoreboard board = each.getScoreboard();

            Team boardTeam = board.getTeam(id);
            if (boardTeam == null) {
                boardTeam = board.registerNewTeam(id);
            }
            boardTeam.setColor(chatColor);
            boardTeam.setPrefix(chatColor + "");


            boardTeam.addEntry(player.getName());
        }
    }
}
