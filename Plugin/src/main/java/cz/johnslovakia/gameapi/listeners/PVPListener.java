package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.events.PlayerDamageByPlayerEvent;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.GamePlayerType;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PVPListener implements Listener {

    //public static Map<GamePlayer, List<GamePlayer>> damagers = new HashMap<>();
    //public static Map<GamePlayer, Map<GamePlayer, Long>> lastDamager = new HashMap<>();

    private final static List<LastDamager> lastDamager = new ArrayList<>();
    private final static List<Damager> damagers = new ArrayList<>();

    public static LastDamager getLastDamager(GamePlayer killed){
        for (LastDamager damagerClass : lastDamager){
            if (damagerClass.getDamaged().equals(killed)){
                return damagerClass;
            }
        }
        return null;
    }

    public static boolean containsLastDamager(GamePlayer killed){
        for (LastDamager damagerClass : lastDamager){
            if (damagerClass.getDamaged().equals(killed)){
                return true;
            }
        }
        return false;
    }

    public void removeLastDamager(GamePlayer killed){
        if (containsLastDamager(killed)){
            lastDamager.remove(getLastDamager(killed));
        }
    }

    public void addLastDamager(GamePlayer killed, GamePlayer dmg){
        if (containsLastDamager(killed)){
            removeLastDamager(killed);
            lastDamager.add(new LastDamager(killed).setLastDamager(dmg, System.currentTimeMillis()));
        }else{
            lastDamager.add(new LastDamager(killed).setLastDamager(dmg, System.currentTimeMillis()));
        }
    }



    public Damager getDamagers(GamePlayer killed){
        for (Damager damagerClass : damagers){
            if (damagerClass.getDamaged().equals(killed)){
                return damagerClass;
            }
        }
        return new Damager(killed);
    }

    public boolean containsKilledInDamager(GamePlayer killed){
        for (Damager damagerClass : damagers){
            if (damagerClass.getDamaged().equals(killed)){
                return true;
            }
        }
        return false;
    }

    public void removeDamager(GamePlayer killed, GamePlayer damager){
        getDamagers(killed).removeDamager(damager);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && ((e.getDamager() instanceof Projectile ||
                e.getDamager() instanceof Player))) {
            GamePlayer player = PlayerManager.getGamePlayer((Player) e.getEntity());

            Optional.ofNullable(player.getPlayerData().getGame()).ifPresent(game -> {
                GamePlayer damager = null;

                if (game.getState() == GameState.INGAME) {
                    if (e.getDamager() instanceof Player){
                        damager = PlayerManager.getGamePlayer((Player) e.getDamager());
                    }else if (e.getDamager() instanceof Arrow projectile) {
                        if (projectile.getShooter() instanceof Player) {
                            damager = PlayerManager.getGamePlayer((Player) projectile.getShooter());
                        }
                    }else if (e.getDamager() instanceof Fireball projectile) {
                        if (projectile.getShooter() instanceof Player) {
                            damager = PlayerManager.getGamePlayer((Player) projectile.getShooter());
                        }
                    }else if (e.getDamager() instanceof Egg projectile) {
                        if (projectile.getShooter() instanceof Player) {
                            damager = PlayerManager.getGamePlayer((Player) projectile.getShooter());
                        }
                    }else if (e.getDamager() instanceof Snowball projectile) {
                        if (projectile.getShooter() instanceof Player) {
                            damager = PlayerManager.getGamePlayer((Player) projectile.getShooter());
                        }
                    }else if (e.getDamager() instanceof LightningStrike projectile) {
                        if (projectile.getCausingPlayer() != null) {
                            damager = PlayerManager.getGamePlayer(projectile.getCausingPlayer());
                        }
                    }else if (e.getDamager() instanceof FishHook) {
                        FishHook projectile = (FishHook) e.getDamager();
                        if (projectile.getShooter() instanceof Player) {
                            damager = PlayerManager.getGamePlayer((Player) projectile.getShooter());
                        }
                    }

                    if (damager == null)return;

                    if (damager.isSpectator()){
                        e.setCancelled(true);
                        return;
                    }

                    addLastDamager(player, damager);

                    Damager damagerClass = getDamagers(player).setDamager(damager, System.currentTimeMillis());
                    if (!damagers.contains(damagerClass)) {
                        damagers.add(damagerClass);
                    }


                    if (!e.isCancelled() && player.getPlayerData().getGame().equals(damager.getPlayerData().getGame())) {

                        if (game.getSettings().isUseTeams()) {
                            //Bylo !=
                            if (player.getPlayerData().getTeam().equals(damager.getPlayerData().getTeam())) {
                                e.setCancelled(true);
                                return;
                            }
                        }

                        if (player.equals(damager)) {
                            e.setCancelled(true);
                            return;
                        }

                        if (!player.isEnabledPVP()) {
                            e.setCancelled(true);
                            return;
                        }

                        PlayerDamageByPlayerEvent ev = new PlayerDamageByPlayerEvent(damager.getPlayerData().getGame(), damager, player, e.getCause());
                        Bukkit.getPluginManager().callEvent(ev);
                        e.setCancelled(ev.isCancelled());

                    }

                } else {
                    e.setCancelled(true);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void entityDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
            Game game = gamePlayer.getPlayerData().getGame();
            if (game != null && game.getState() == GameState.INGAME) {
                if (gamePlayer.isSpectator()){
                    player.teleport(RespawnListener.getNonRespawnLocation(game));
                    e.setCancelled(true);
                    return;
                }
                PlayerDamageByPlayerEvent ev = new PlayerDamageByPlayerEvent(game, null, PlayerManager.getGamePlayer(player), e.getCause());
                Bukkit.getPluginManager().callEvent(ev);
                e.setCancelled(ev.isCancelled());
            }else{
                e.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onForceRespawn(PlayerDeathEvent e){
        GameAPI.getInstance().getVersionSupport().forceRespawn(GameAPI.getInstance(), e.getEntity());
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((GameAPI.getInstance()), () -> {
            e.getEntity().spigot().respawn();
            e.getEntity().setFireTicks(0);
        }, 2L);
    }



    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        if (gamePlayer.getType().equals(GamePlayerType.DISCONNECTED)){
            e.setKeepInventory(true);
            e.setKeepLevel(true);
        }

        gamePlayer.getMetadata().put("death_location", player.getLocation());

        boolean killer = false;
        if (containsLastDamager(gamePlayer)){
            if (System.currentTimeMillis() - getLastDamager(gamePlayer).getMs() <= 12000){
                killer = true;
            }
        }

        if (killer){
            GamePlayer gamePlayerKiller = getLastDamager(gamePlayer).getLastDamager();

            List<GamePlayer> assists = new ArrayList<>();
            Damager d = getDamagers(gamePlayer);
            for (GamePlayer dgp : d.getDamagers().keySet()){
                if (System.currentTimeMillis() - getDamagers(gamePlayer).getLong(dgp) <= 12000){
                    if (dgp.equals(gamePlayerKiller)){
                        continue;
                    }
                    assists.add(dgp);
                }
            }

            GamePlayerDeathEvent ev = new GamePlayerDeathEvent(gamePlayer.getPlayerData().getGame(), gamePlayerKiller, PlayerManager.getGamePlayer(player), (!assists.isEmpty() ? assists : null), (player.getLastDamageCause() != null ? (player.getLastDamageCause() != null ? player.getLastDamageCause().getCause() : null) : null));
            if (gamePlayer.getPlayerData().getGame().isFirstGameKill()){
                ev.setFirstGameKill(true);
                gamePlayer.getPlayerData().getGame().setFirstGameKill(false);
            }
            Bukkit.getPluginManager().callEvent(ev);

            removeLastDamager(gamePlayer);
            getDamagers(gamePlayer).removeAllDamagers();
        } else {
            if (player.getLastDamageCause() == null) {
                GamePlayerDeathEvent ev = new GamePlayerDeathEvent(gamePlayer.getPlayerData().getGame(), null, PlayerManager.getGamePlayer(player), null,null);
                Bukkit.getPluginManager().callEvent(ev);
            } else {
                GamePlayerDeathEvent ev = new GamePlayerDeathEvent(gamePlayer.getPlayerData().getGame(), null, PlayerManager.getGamePlayer(player), null, (player.getLastDamageCause() != null ? player.getLastDamageCause().getCause() : null));
                Bukkit.getPluginManager().callEvent(ev);
            }
        }


        e.setDeathMessage(null);
    }

    public static final HashMap<Player, Location> deathLocations = new HashMap<>();

    @EventHandler
    public void onPlayerDeath2(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation();
        deathLocations.put(player, deathLocation);
    }


    @Getter
    public static class Damager {
        private final GamePlayer damaged;
        private final Map<GamePlayer, Long> damagers = new HashMap<>();

        public Damager(GamePlayer damaged) {
            this.damaged = damaged;
        }

        public Damager setDamager(GamePlayer damager, long ms){
            damagers.put(damager, ms);
            return this;
        }

        public Damager removeDamager(GamePlayer damager){
            damagers.remove(damager);
            return this;
        }

        public Long getLong(GamePlayer damager){
            if (!damagers.containsKey(damager)){
                return null;
            }
            return damagers.get(damager);
        }

        public void removeAllDamagers(){
            damagers.clear();
        }

    }

    @Getter
    public static class LastDamager {

        private long ms;
        private final GamePlayer damaged;
        private GamePlayer lastDamager;

        public LastDamager(GamePlayer damaged) {
            this.damaged = damaged;
        }

        public LastDamager setLastDamager(GamePlayer lastDamager, long ms){
            this.lastDamager = lastDamager;
            this.ms = ms;
            return this;
        }
    }
}