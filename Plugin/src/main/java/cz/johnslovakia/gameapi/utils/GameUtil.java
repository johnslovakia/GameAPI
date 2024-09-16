package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.map.MapLocation;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameUtil {

    public ItemStack getPotionItemStack(PotionType type, int level, boolean extend, boolean upgraded, boolean splash, String displayName){
        ItemStack potion = new ItemStack((splash ? Material.SPLASH_POTION : Material.POTION), 1);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);
        return potion;
    }

    public static String getDurationString(int seconds) {

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = (seconds % 3600) % 60;

        //int minutes = (seconds % 3600) / 60;
        //seconds = seconds % 60;

        return (hours > 1 ? hours : "") + twoDigitString(minutes) + ":" + twoDigitString(seconds);
    }

    private static String twoDigitString(int number) {

        if (number == 0) {
            return "00";
        }

        if (number / 10 == 0) {
            return "0" + number;
        }

        return String.valueOf(number);
    }

    static public String getStringLocation(final Location l, boolean world) {
        if (l == null) {
            return "";
        }
        return (world ? l.getWorld().getName() : "") + l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ() + ";" + l.getYaw() + ";" + l.getPitch();
    }

    static public MapLocation getMapLocationFromString(String id, String s, boolean yaw_and_pitch) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        final String[] parts = s.split(";");
        if (parts.length == 5) {
            final int x = Integer.parseInt(parts[0]);
            final int y = Integer.parseInt(parts[1]);
            final int z = Integer.parseInt(parts[2]);
            final int yaw = Integer.parseInt(parts[3]);
            final int pitch = Integer.parseInt(parts[4]);
            if (yaw_and_pitch) {
                return new MapLocation(id, x, y, z, yaw, pitch);
            }else{
                return new MapLocation(id, x, y, z);
            }
        }else if (parts.length == 6) {
            final String world = parts[0];
            final int x = Integer.parseInt(parts[1]);
            final int y = Integer.parseInt(parts[2]);
            final int z = Integer.parseInt(parts[3]);
            final int yaw = Integer.parseInt(parts[4]);
            final int pitch = Integer.parseInt(parts[5]);
            if (yaw_and_pitch) {
                return new MapLocation(id, world, x, y, z, yaw, pitch);
            }else{
                return new MapLocation(id, world, x, y, z);
            }
        }
        return null;
    }

    static public Location getLocationString(final String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        final String[] parts = s.split(":");
        if (parts.length == 4) {
            final World w = Bukkit.getServer().getWorld(parts[0]);
            final int x = Integer.parseInt(parts[1]);
            final int y = Integer.parseInt(parts[2]);
            final int z = Integer.parseInt(parts[3]);
            return new Location(w, x, y, z);
        }else if (parts.length == 6) {
            final World w = Bukkit.getServer().getWorld(parts[0]);
            final int x = Integer.parseInt(parts[1]);
            final int y = Integer.parseInt(parts[2]);
            final int z = Integer.parseInt(parts[3]);
            final int yaw = Integer.parseInt(parts[4]);
            final int pitch = Integer.parseInt(parts[5]);
            return new Location(w, x, y, z, yaw, pitch);
        }
        return null;
    }

    public static void sendToLobby(Player player){
        ConfigAPI config = new ConfigAPI(GameAPI.getInstance().getMinigameDataFolder().toString(), "config.yml", GameAPI.getInstance());
        List<String> lobbies = config.getConfig().getStringList("lobby_servers");

        player.sendMessage("§7Sending to lobby...");
        new BukkitRunnable(){
            @Override
            public void run() {
                Collections.shuffle(lobbies);
                send(player, lobbies.get(0));
            }
        }.runTaskAsynchronously(GameAPI.getInstance());
    }

    public static void send(Player player, String server) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("Connect");
            out.writeUTF(server.toLowerCase());
        } catch (IOException localIOException) {
            player.kickPlayer(MessageManager.get(player, "kick.offline_server").getTranslated());
        }
        player.sendPluginMessage(GameAPI.getInstance(), "BungeeCord", b.toByteArray());
    }

    public static void colorizeArmor(GamePlayer gamePlayer) {
        Player player = gamePlayer.getOnlinePlayer();
        PlayerInventory inv = player.getInventory();
        GameTeam team = gamePlayer.getPlayerData().getTeam();

        if (team == null || inv.getArmorContents() == null){
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

    public static Map<GamePlayer, Integer> getTopPlayers(Game game, String scoreName, int maxResults) {
        List<GamePlayer> gamePlayers = game.getParticipants();
        gamePlayers.sort((p1, p2) -> Integer.compare(p2.getScoreByName(scoreName).getScore(), p1.getScoreByName(scoreName).getScore()));

        Map<GamePlayer, Integer> result = new HashMap<>();
        int rank = 1;
        int lastKills = -1;
        int count = 0;

        for (int i = 0; i < gamePlayers.size() && count < maxResults; i++) {
            GamePlayer gamePlayer = gamePlayers.get(i);

            if (gamePlayer.getScoreByName(scoreName).getScore() != lastKills) {
                rank = i + 1;
                lastKills = gamePlayer.getScoreByName(scoreName).getScore();
            }

            result.put(gamePlayer, rank);
            count++;
        }

        return result;
    }

    public static void countdownTimerBar(GamePlayer gamePlayer, double startTime, double timeRemaining){
        countdownTimerBar(gamePlayer, "", startTime, timeRemaining);
    }

    public static void countdownTimerBar(GamePlayer gamePlayer, String name, double startTime, double timeRemaining){
        String BAR_CHAR = "|";

        StringBuilder text = new StringBuilder();
        text.append(ChatColor.GRAY);
        text.append((name != null ? name : ""));
        if (timeRemaining < 0) {
            return;
        }
        int fullDisplay = 30;
        double timeIncompleted = (fullDisplay * (timeRemaining / startTime));
        double timeCompleted = fullDisplay - timeIncompleted;
        text.append(ChatColor.GREEN);
        for (int i = 0; i < (int) timeCompleted; i++) {
            text.append(BAR_CHAR);
        }
        text.append(ChatColor.RED);
        for (int i = 0; i < (int) timeIncompleted; i++) {
            text.append(BAR_CHAR);
        }
        text.append(ChatColor.GRAY);
        text.append(' ');
        double roundedDouble = Math.round(timeRemaining * 100.0) / 100.0;
        text.append(roundedDouble);
        text.append("s");

        if (gamePlayer != null && gamePlayer.isOnline()) GameAPI.getInstance().getUserInterface().sendAction(gamePlayer.getOnlinePlayer(), text.toString());
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
}
