package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.map.GameMap;
import cz.johnslovakia.gameapi.modules.game.map.MapLocation;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.scores.ScoreModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.attribute.Attribute;
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

    public static void hideAndShowPlayers(GameInstance game, Player player){
        for (Player serverPlayer : Bukkit.getOnlinePlayers()){
            player.hidePlayer(Shared.getInstance().getPlugin(), serverPlayer);
            serverPlayer.hidePlayer(Shared.getInstance().getPlugin(), player);

            for (GamePlayer gamePlayer : game.getParticipants()){
                Player p = gamePlayer.getOnlinePlayer();
                p.showPlayer(Shared.getInstance().getPlugin(), player);
                player.showPlayer(Shared.getInstance().getPlugin(), p);
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

        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> player.kick(ModuleManager.getModule(MessageModule.class).get(player, "kick.offline_server").getTranslated()), 60L);
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

    public static Map<GamePlayer, Integer> getTopPlayers(GameInstance game, String scoreName, int maxResults) {
        ScoreModule scoreModule = ModuleManager.getModule(ScoreModule.class);

        List<GamePlayer> gamePlayers = game.getParticipants();
        gamePlayers.sort((p1, p2) -> Integer.compare(p2.getGameSession().getScore(scoreName), p1.getGameSession().getScore(scoreName)));

        Map<GamePlayer, Integer> result = new HashMap<>();
        int rank = 1;
        int lastKills = -1;
        int count = 0;

        for (int i = 0; i < gamePlayers.size() && count < maxResults; i++) {
            GamePlayer gamePlayer = gamePlayers.get(i);

            if (gamePlayer.getGameSession().getScore(scoreName) == 0){
                continue;
            }

            if (gamePlayer.getGameSession().getScore(scoreName) != lastKills) {
                rank = i + 1;
                lastKills = gamePlayer.getGameSession().getScore(scoreName);
            }

            result.put(gamePlayer, rank);
            count++;
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

    public static void damagePlayer(Player player, double damage) {
        if (PlayerManager.getGamePlayer(player).isSpectator()){
            return;
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

        player.damage(Utils.calculateDamageApplied(damage, points, toughness, resistance, epf));
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
