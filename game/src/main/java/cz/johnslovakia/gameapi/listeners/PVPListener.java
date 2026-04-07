package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.map.AreaManager;
import cz.johnslovakia.gameapi.modules.game.map.AreaSettings;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.events.PlayerDamageByPlayerEvent;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.GamePlayerState;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.GameUtils;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.Utils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;

import java.util.*;

public class PVPListener implements Listener {

    private final static List<LastDamager> lastDamager = new ArrayList<>();
    private final static List<Damager> damagers = new ArrayList<>();

    public static final int KILL_CREDIT_TIMEOUT_MS = 12_000;

    public static LastDamager getLastDamager(GamePlayer killed){
        for (LastDamager damagerClass : lastDamager){
            if (damagerClass.getDamaged().equals(killed)){
                return damagerClass;
            }
        }
        return null;
    }

    public static boolean hasLastDamager(GamePlayer killed){
        for (LastDamager damagerClass : lastDamager){
            if (damagerClass.getDamaged().equals(killed)){
                return true;
            }
        }
        return false;
    }

    public void removeLastDamager(GamePlayer killed){
        if (hasLastDamager(killed)){
            lastDamager.remove(getLastDamager(killed));
        }
    }

    public void addLastDamager(GamePlayer killed, GamePlayer dmg){
        if (hasLastDamager(killed)){
            removeLastDamager(killed);
            lastDamager.add(new LastDamager(killed).setLastDamager(dmg, System.currentTimeMillis()));
        }else{
            lastDamager.add(new LastDamager(killed).setLastDamager(dmg, System.currentTimeMillis()));
        }
    }

    public static void cleanupPlayer(GamePlayer gamePlayer) {
        lastDamager.removeIf(d -> d.getDamaged().equals(gamePlayer));
        damagers.removeIf(d -> d.getDamaged().equals(gamePlayer));
    }



    public static Damager getDamagers(GamePlayer killed){
        for (Damager damagerClass : damagers){
            if (damagerClass.getDamaged().equals(killed)){
                return damagerClass;
            }
        }
        return new Damager(killed);
    }

    public void removeDamager(GamePlayer killed, GamePlayer damager){
        getDamagers(killed).removeDamager(damager);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        GamePlayer player = PlayerManager.getGamePlayer(victim);
        if (player == null) return;

        GameInstance game = player.getGame();
        if (game == null || game.getState() != GameState.INGAME || game.isPreparation() || player.isSpectator()) {
            e.setCancelled(true);
            return;
        }

        AreaSettings settings = AreaManager.getActiveSettings(player);
        if (settings != null){
            if (!settings.isCanPvP()){
                e.setCancelled(true);
                return;
            }
        }


        DamageRecord damageRecord = getDamagerFromEntity(e);
        if (damageRecord == null) return;
        GamePlayer damager = damageRecord.damager;

        if (damager.isSpectator() || !player.getGameSession().isEnabledPVP()){
            e.setCancelled(true);
            return;
        }
        if (player.equals(damager)){
            return;
        }
        if (Minigame.getInstance().getSettings().isUseTeams()){
            if(!player.equals(damager) && player.getGameSession().getTeam() != null && damager.getGameSession().getTeam() != null && player.getGameSession().getTeam().equals(damager.getGameSession().getTeam())){
                e.setCancelled(true);
                return;
            }
        }

        addLastDamager(player, damager);
        Damager damagerClass = getDamagers(player);
        damagerClass.setDamager(damager, System.currentTimeMillis());
        if (!damagers.contains(damagerClass)) {
            damagers.add(damagerClass);
        }

        PlayerDamageByPlayerEvent ev = new PlayerDamageByPlayerEvent(game, damager, player, e.getCause());
        Bukkit.getPluginManager().callEvent(ev);
        e.setCancelled(ev.isCancelled());
    }

    public record DamageRecord(GamePlayer damager, EntityDamageEvent.DamageCause cause) {}

    private DamageRecord getDamagerFromEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        if (damager instanceof Player p) return new DamageRecord(PlayerManager.getGamePlayer(p), event.getCause());

        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                if (projectile instanceof Egg || projectile instanceof Snowball) {
                    handleKnockbackProjectile(projectile, shooter);
                    return new DamageRecord(PlayerManager.getGamePlayer(shooter), event.getCause());
                }
                return new DamageRecord(PlayerManager.getGamePlayer(shooter), event.getCause());
            }
        }

        if (damager instanceof LightningStrike lightning && lightning.getCausingPlayer() != null) {
            return new DamageRecord(PlayerManager.getGamePlayer(lightning.getCausingPlayer()), event.getCause());
        }

        if (damager instanceof FishHook hook && hook.getShooter() instanceof Player shooter) {
            return new DamageRecord(PlayerManager.getGamePlayer(shooter), event.getCause());
        }

        if (damager instanceof TNTPrimed tnt) {
            if (tnt.hasMetadata("Source")) {
                List<MetadataValue> meta = tnt.getMetadata("Source");
                for (MetadataValue value : meta) {
                    if (value.value() instanceof Player source) {
                        return new DamageRecord(PlayerManager.getGamePlayer(source), event.getCause());
                    }
                }
            }
            if (tnt.getSource() instanceof Player source) {
                return new DamageRecord(PlayerManager.getGamePlayer(source), event.getCause());
            }
        }
        return null;
    }

    private void handleKnockbackProjectile(Projectile projectile, Player shooter) {
        Entity hit = projectile.getNearbyEntities(1, 1, 1).stream()
                .filter(entity -> entity instanceof Player)
                .findFirst()
                .orElse(null);

        if (!(hit instanceof Player victim)) return;

        GamePlayer victimGP = PlayerManager.getGamePlayer(victim);
        if (victimGP == null) return;

        Vector direction = victim.getLocation().toVector().subtract(shooter.getLocation().toVector());
        if (direction.lengthSquared() == 0) return;
        direction = direction.normalize();

        victim.setVelocity(direction.multiply(0.7));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void entityDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player player) {
            GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
            if (!gamePlayer.isInGame()) return;
            GameInstance game = gamePlayer.getGame();
            if (game.getState() == GameState.INGAME) {
                if (gamePlayer.isSpectator() && (e.getCause().equals(EntityDamageEvent.DamageCause.VOID)  || e.getDamageSource().getDamageType().equals(DamageType.OUT_OF_WORLD))){
                    e.setCancelled(true);
                    player.teleport(GameUtils.getNonRespawnLocation(game));
                    return;
                }
                PlayerDamageByPlayerEvent ev = new PlayerDamageByPlayerEvent(game, null, PlayerManager.getGamePlayer(player), e.getCause());
                Bukkit.getPluginManager().callEvent(ev);
                if (ev.isCancelled()) e.setCancelled(true);
            }else{
                e.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void forceRespawn(PlayerDeathEvent e) {
        if (e.isCancelled()) return;

        Player player = e.getEntity();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        if (gamePlayer == null || !gamePlayer.isInGame()) return;

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(
                Minigame.getInstance().getPlugin(), () -> {
                    if (!player.isOnline()) return;
                    player.spigot().respawn();
                    player.setFireTicks(0);
                }, 2L
        );
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent e){
        Player player = e.getEntity();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        if (!gamePlayer.isInGame()) return;
        GameInstance game = gamePlayer.getGame();
        if (!game.getState().equals(GameState.INGAME)) return;
        if (gamePlayer.isSpectator()) {
            e.setCancelled(true);
            return;
        }

        e.getDrops().removeIf(item ->
                item.getType() == Material.CARVED_PUMPKIN &&
                        item.hasItemMeta() &&
                        item.getItemMeta().hasCustomModelData()
        );

        if (gamePlayer.getGameSession().getState().equals(GamePlayerState.DISCONNECTED)){
            e.setKeepInventory(true);
            e.setKeepLevel(true);
        }

        gamePlayer.getMetadata().put("death_location", player.getLocation());

        boolean killer = false;
        if (hasLastDamager(gamePlayer)) {
            GamePlayer last = getLastDamager(gamePlayer).getLastDamager();
            if (last != null && last != gamePlayer) {
                if (System.currentTimeMillis() - getLastDamager(gamePlayer).getMs() <= KILL_CREDIT_TIMEOUT_MS){
                    killer = true;
                }
            }
        }

        DamageType damageType = e.getDamageSource().getDamageType();
        if (gamePlayer.getMetadata().containsKey("diedInVoid")){
            damageType = DamageType.OUT_OF_WORLD;
            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> gamePlayer.getMetadata().remove("diedInVoid"), 10L);
        }

        if (killer){
            GamePlayer gamePlayerKiller = getLastDamager(gamePlayer).getLastDamager();
            List<GamePlayer> assists = getAssists(gamePlayer, gamePlayerKiller);

            GamePlayerDeathEvent ev = new GamePlayerDeathEvent(game, gamePlayerKiller, PlayerManager.getGamePlayer(player), (!assists.isEmpty() ? assists : null), damageType, e.getDrops());
            if (game.isFirstGameKill()){
                ev.setFirstGameKill(true);
                game.setFirstGameKill(false);
            }
            Bukkit.getPluginManager().callEvent(ev);

            removeLastDamager(gamePlayer);
            getDamagers(gamePlayer).removeAllDamagers();
        } else {
            if (player.getLastDamageCause() == null) {
                GamePlayerDeathEvent ev = new GamePlayerDeathEvent(game, null, PlayerManager.getGamePlayer(player), null,null, new ArrayList<>(e.getDrops()));
                Bukkit.getPluginManager().callEvent(ev);
            } else {
                GamePlayerDeathEvent ev = new GamePlayerDeathEvent(game, null, PlayerManager.getGamePlayer(player), null, damageType, new ArrayList<>(e.getDrops()));
                Bukkit.getPluginManager().callEvent(ev);
            }
        }


        cleanupPlayer(gamePlayer);

        e.setDeathMessage(null);
    }

    public static List<GamePlayer> getAssists(GamePlayer gamePlayer, GamePlayer killer){
        List<GamePlayer> assists = new ArrayList<>();
        Damager d = getDamagers(gamePlayer);
        for (GamePlayer dgp : d.getDamagers().keySet()){
            if (System.currentTimeMillis() - getDamagers(gamePlayer).getLong(dgp) <= KILL_CREDIT_TIMEOUT_MS){
                if (dgp.equals(killer)) continue;
                assists.add(dgp);
            }
        }
        return assists;
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

        public void removeDamager(GamePlayer damager){
            damagers.remove(damager);
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