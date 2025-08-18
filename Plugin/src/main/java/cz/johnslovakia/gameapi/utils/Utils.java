package cz.johnslovakia.gameapi.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.game.map.MapLocation;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

public class Utils {

    public static int getPrice(FileConfiguration config, String path, int defaultPrice){
        int price = config.getInt(path);
        return (price != 0 ? price : defaultPrice);
    }

    public static void spawnFireworks(Location location, int amount, Color color, FireworkEffect.Type type){
        Firework fw = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
        FireworkMeta fwm = fw.getFireworkMeta();

        fwm.setPower(2);
        fwm.addEffect(FireworkEffect.builder().withColor(color).flicker(true).with(type).build());
        fw.setFireworkMeta(fwm);
        fw.detonate();

        for(int i = 0;i < amount; i++){
            Firework fw2 = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
            fw2.setFireworkMeta(fwm);
        }
    }

    public static boolean isPotion(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        return item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION;
    }


    public static void levitatePlayer(Player player, int level, int blocks) {
        double duration = (blocks / (0.9 + 0.9 * level)) * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (int) duration, level));
    }

    public ItemStack getPotionItemStack(PotionType type, int level, boolean extend, boolean upgraded, boolean splash, String displayName){
        ItemStack potion = new ItemStack((splash ? Material.SPLASH_POTION : Material.POTION), 1);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);
        return potion;
    }

    public static Inventory copyPlayerInventory(Player player) {
        Inventory original = player.getInventory();
        Inventory copy = Bukkit.createInventory(null, InventoryType.PLAYER);

        for (int i = 0; i < original.getSize(); i++) {
            ItemStack item = original.getItem(i);
            if (item != null) {
                copy.setItem(i, item.clone());
            }
        }

        return copy;
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
        return (world ? l.getWorld().getName() + ";": "") + l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ() + ";" + l.getYaw() + ";" + l.getPitch();
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

    static public Location getLocationString(final String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        final String[] parts = s.split(";");
        if (parts.length == 4) {
            final World w = Bukkit.getServer().getWorld(parts[0]);
            final double x = Double.parseDouble(parts[1]);
            final double y = Double.parseDouble(parts[2]);
            final double z = Double.parseDouble(parts[3]);
            return new Location(w, x, y, z);
        }else if (parts.length == 6) {
            final World w = Bukkit.getServer().getWorld(parts[0]);
            final double x = Double.parseDouble(parts[1]);
            final double y = Double.parseDouble(parts[2]);
            final double z = Double.parseDouble(parts[3]);
            final float yaw = Float.parseFloat(parts[4]);
            final float pitch =  Float.parseFloat(parts[5]);
            return new Location(w, x, y, z, yaw, pitch);
        }
        Logger.log("getLocationString Error: " + parts.length + " " + s, Logger.LogType.ERROR);
        return null;
    }

    public static void hideAndShowPlayers(GamePlayer gamePlayer){
        Player player = gamePlayer.getOnlinePlayer();

        for (Player serverPlayer : Bukkit.getOnlinePlayers()){
            GameAPI.getInstance().getVersionSupport().hidePlayer(Minigame.getInstance().getPlugin(), serverPlayer, player);
            GameAPI.getInstance().getVersionSupport().hidePlayer(Minigame.getInstance().getPlugin(), player, serverPlayer);

            for (GamePlayer gp : gamePlayer.getGame().getParticipants()){
                Player p = gp.getOnlinePlayer();
                GameAPI.getInstance().getVersionSupport().showPlayer(Minigame.getInstance().getPlugin(), p, player);
                GameAPI.getInstance().getVersionSupport().showPlayer(Minigame.getInstance().getPlugin(), player, p);
            }
        }
    }

    public static void sendToLobby(Player player){
        sendToLobby(player, true);
    }

    public static void sendToLobby(Player player, boolean message){
        ConfigAPI config = new ConfigAPI(GameAPI.getInstance().getMinigameDataFolder().toString(), "config.yml", Minigame.getInstance().getPlugin());
        List<String> lobbies = config.getConfig().getStringList("lobby_servers");

        if (message) MessageManager.get(player, "chat.sending_to_lobby").send();

        Collections.shuffle(lobbies);
        send(player, (lobbies.isEmpty() ? "Lobby" : lobbies.get(0)));

        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> player.kick(MessageManager.get(player, "kick.offline_server").getTranslated()), 60L);
    }

    public static void send(Player player, String server) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("Connect");
            out.writeUTF(server.toLowerCase());
        } catch (Exception exception) {
            player.kick(MessageManager.get(player, "kick.offline_server").add(" §8(" + exception.getMessage() + ")§r").getTranslated());
        }
        player.sendPluginMessage(Minigame.getInstance().getPlugin(), "BungeeCord", b.toByteArray());
    }

    public static void colorizeArmor(GamePlayer gamePlayer) {
        Player player = gamePlayer.getOnlinePlayer();
        PlayerInventory inv = player.getInventory();
        GameTeam team = gamePlayer.getTeam();

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

            if (gamePlayer.getScoreByName(scoreName).getScore() == 0){
                continue;
            }

            if (gamePlayer.getScoreByName(scoreName).getScore() != lastKills) {
                rank = i + 1;
                lastKills = gamePlayer.getScoreByName(scoreName).getScore();
            }

            result.put(gamePlayer, rank);
            count++;
        }

        return result;
    }

    public static String getStringProgressBar(int current, int required) {
        int totalBars = 25;
        float progress = (float) current / required;
        int filledBars = Math.round(progress * totalBars);

        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < totalBars; i++) {
            if (i > 0 && i % 5 == 0) {
                bar.append(" ");
            }

            if (i < filledBars) {
                bar.append(ChatColor.GREEN + "|");
            } else {
                bar.append(ChatColor.DARK_GRAY + "|");
            }
        }

        int percent = Math.round(progress * 100);
        bar.append(" ").append(ChatColor.GRAY).append(percent).append("%");

        return bar.toString();
    }

    public static void countdownTimerBar(GamePlayer gamePlayer, double startTime, double timeRemaining){
        countdownTimerBar(gamePlayer, "", startTime, timeRemaining);
    }

    public static ItemStack getCustomHead(String url) {
        url = url.toLowerCase();
        url = url.replace("http://textures.minecraft.net/texture/", "");
        url = url.replace("https://textures.minecraft.net/texture/", "");


        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();

        PlayerProfile pp = Bukkit.createProfile(UUID.fromString("4fbecd49-c7d4-4c18-8410-adf7a7348728"));
        PlayerTextures pt = pp.getTextures();

        try {
            pt.setSkin(new URL("http://textures.minecraft.net/texture/" + url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        pp.setTextures(pt);
        skullMeta.setPlayerProfile(pp);
        item.setItemMeta(skullMeta);
        return item;
    }

    public static ItemStack getPlayerHead(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));
        item.setItemMeta(meta);
        return item;
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

        if (gamePlayer != null && gamePlayer.isOnline()) ActionBarManager.sendActionBar(gamePlayer.getOnlinePlayer(), text.toString());
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

    private static double calculateDamageApplied(double damage, double points, double toughness, int resistance, int epf) {
        double withArmorAndToughness = damage * (1 - Math.min(20, Math.max(points / 5, points - damage / (2 + toughness / 4))) / 25);
        double withResistance = withArmorAndToughness * (1 - (resistance * 0.2));

        return withResistance * (1 - (Math.min(20.0, epf) / 25));
    }

    private static int getEPF(PlayerInventory inv) {
        ItemStack helm = inv.getHelmet();
        ItemStack chest = inv.getChestplate();
        ItemStack legs = inv.getLeggings();
        ItemStack boot = inv.getBoots();

        return (helm != null ? helm.getEnchantmentLevel(Enchantment.SHARPNESS) : 0) +
                (chest != null ? chest.getEnchantmentLevel(Enchantment.SHARPNESS) : 0) +
                (legs != null ? legs.getEnchantmentLevel(Enchantment.SHARPNESS) : 0) +
                (boot != null ? boot.getEnchantmentLevel(Enchantment.SHARPNESS) : 0);
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
        int epf = getEPF(player.getInventory());

        player.damage(calculateDamageApplied(damage, points, toughness, resistance, epf));
    }
}
