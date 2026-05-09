package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.map.AreaManager;
import cz.johnslovakia.gameapi.modules.game.map.AreaSettings;
import cz.johnslovakia.gameapi.events.GamePlayerDeathEvent;
import cz.johnslovakia.gameapi.events.PlayerDamageByPlayerEvent;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.GameUtils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
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

    // -------------------------------------------------------------------------
    // LastDamager / Damager tracking
    // -------------------------------------------------------------------------

    public static LastDamager getLastDamager(GamePlayer killed) {
        for (LastDamager d : lastDamager) {
            if (d.getDamaged().equals(killed)) return d;
        }
        return null;
    }

    public static boolean hasLastDamager(GamePlayer killed) {
        for (LastDamager d : lastDamager) {
            if (d.getDamaged().equals(killed)) return true;
        }
        return false;
    }

    public void removeLastDamager(GamePlayer killed) {
        if (hasLastDamager(killed)) lastDamager.remove(getLastDamager(killed));
    }

    public void addLastDamager(GamePlayer killed, GamePlayer dmg) {
        removeLastDamager(killed);
        lastDamager.add(new LastDamager(killed).setLastDamager(dmg, System.currentTimeMillis()));
    }

    public static Damager getDamagers(GamePlayer killed) {
        for (Damager d : damagers) {
            if (d.getDamaged().equals(killed)) return d;
        }
        return new Damager(killed);
    }

    public void removeDamager(GamePlayer killed, GamePlayer damager) {
        getDamagers(killed).removeDamager(damager);
    }

    public static void cleanupPlayer(GamePlayer gamePlayer) {
        lastDamager.removeIf(d -> d.getDamaged().equals(gamePlayer));
        damagers.removeIf(d -> d.getDamaged().equals(gamePlayer));
    }

    // -------------------------------------------------------------------------
    // PvP damage handling
    // -------------------------------------------------------------------------

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
        if (settings != null && !settings.isCanPvP()) {
            e.setCancelled(true);
            return;
        }

        DamageRecord damageRecord = getDamagerFromEntity(e);
        if (damageRecord == null) return;
        GamePlayer damager = damageRecord.damager;

        if (damager.isSpectator() || !player.getGameSession().isEnabledPVP()) {
            e.setCancelled(true);
            return;
        }
        if (player.equals(damager)) return;

        if (Minigame.getInstance().getSettings().isUseTeams()) {
            if (player.getGameSession().getTeam() != null
                    && damager.getGameSession().getTeam() != null
                    && player.getGameSession().getTeam().equals(damager.getGameSession().getTeam())) {
                e.setCancelled(true);
                return;
            }
        }

        addLastDamager(player, damager);
        Damager damagerClass = getDamagers(player);
        damagerClass.setDamager(damager, System.currentTimeMillis());
        if (!damagers.contains(damagerClass)) damagers.add(damagerClass);

        PlayerDamageByPlayerEvent ev = new PlayerDamageByPlayerEvent(game, damager, player, e.getCause());
        Bukkit.getPluginManager().callEvent(ev);
        e.setCancelled(ev.isCancelled());
    }

    public record DamageRecord(GamePlayer damager, EntityDamageEvent.DamageCause cause) {}

    private DamageRecord getDamagerFromEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        if (damager instanceof Player p)
            return new DamageRecord(PlayerManager.getGamePlayer(p), event.getCause());

        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            if (projectile instanceof Egg || projectile instanceof Snowball)
                handleKnockbackProjectile(projectile, shooter);
            return new DamageRecord(PlayerManager.getGamePlayer(shooter), event.getCause());
        }

        if (damager instanceof LightningStrike lightning && lightning.getCausingPlayer() != null)
            return new DamageRecord(PlayerManager.getGamePlayer(lightning.getCausingPlayer()), event.getCause());

        if (damager instanceof FishHook hook && hook.getShooter() instanceof Player shooter)
            return new DamageRecord(PlayerManager.getGamePlayer(shooter), event.getCause());

        if (damager instanceof TNTPrimed tnt) {
            if (tnt.hasMetadata("Source")) {
                for (MetadataValue value : tnt.getMetadata("Source")) {
                    if (value.value() instanceof Player source)
                        return new DamageRecord(PlayerManager.getGamePlayer(source), event.getCause());
                }
            }
            if (tnt.getSource() instanceof Player source)
                return new DamageRecord(PlayerManager.getGamePlayer(source), event.getCause());
        }
        return null;
    }

    private void handleKnockbackProjectile(Projectile projectile, Player shooter) {
        Entity hit = projectile.getNearbyEntities(1, 1, 1).stream()
                .filter(e -> e instanceof Player).findFirst().orElse(null);
        if (!(hit instanceof Player victim)) return;
        if (PlayerManager.getGamePlayer(victim) == null) return;

        Vector direction = victim.getLocation().toVector().subtract(shooter.getLocation().toVector());
        if (direction.lengthSquared() == 0) return;
        victim.setVelocity(direction.normalize().multiply(0.7));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void entityDamage(EntityDamageEvent e) {
        if (e instanceof EntityDamageByEntityEvent) return;
        if (!(e.getEntity() instanceof Player player)) return;

        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        if (!gamePlayer.isInGame()) return;
        GameInstance game = gamePlayer.getGame();

        if (game.getState() == GameState.INGAME) {
            if (gamePlayer.isSpectator() && (e.getCause().equals(EntityDamageEvent.DamageCause.VOID)
                    || e.getDamageSource().getDamageType().equals(DamageType.OUT_OF_WORLD))) {
                e.setCancelled(true);
                player.teleport(GameUtils.getNonRespawnLocation(game));
                return;
            }
            PlayerDamageByPlayerEvent ev = new PlayerDamageByPlayerEvent(game, null, gamePlayer, e.getCause());
            Bukkit.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) e.setCancelled(true);
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        if (!gamePlayer.isInGame()) return;
        GameInstance game = gamePlayer.getGame();
        if (!game.getState().equals(GameState.INGAME)) return;
        if (gamePlayer.isSpectator()) return;

        e.setCancelled(true);
        e.setDeathMessage(null);

        Location deathLoc = player.getLocation();
        gamePlayer.getMetadata().put("death_location", deathLoc);

        List<ItemStack> drops = new ArrayList<>(e.getDrops());
        drops.removeIf(item ->
                item != null
                && item.getType() == Material.CARVED_PUMPKIN
                && item.hasItemMeta()
                && item.getItemMeta().hasCustomModelData()
        );

        for (ItemStack drop : drops) {
            if (drop != null && drop.getType() != Material.AIR) {
                player.getWorld().dropItemNaturally(deathLoc, drop);
            }
        }

        player.getInventory().clear();
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFireTicks(0);
        player.setArrowsInBody(0);
        player.setInvulnerable(false);

        boolean hasKiller = false;
        if (hasLastDamager(gamePlayer)) {
            GamePlayer last = getLastDamager(gamePlayer).getLastDamager();
            if (last != null && last != gamePlayer
                    && System.currentTimeMillis() - getLastDamager(gamePlayer).getMs() <= KILL_CREDIT_TIMEOUT_MS) {
                hasKiller = true;
            }
        }

        DamageType damageType = e.getDamageSource().getDamageType();
        if (gamePlayer.getMetadata().containsKey("diedInVoid")) {
            damageType = DamageType.OUT_OF_WORLD;
            gamePlayer.getMetadata().remove("diedInVoid");
        }

        if (hasKiller) {
            GamePlayer killer = getLastDamager(gamePlayer).getLastDamager();
            List<GamePlayer> assists = getAssists(gamePlayer, killer);

            GamePlayerDeathEvent ev = new GamePlayerDeathEvent(
                    game, killer, PlayerManager.getGamePlayer(player),
                    assists.isEmpty() ? null : assists, damageType, drops);
            if (game.isFirstGameKill()) {
                ev.setFirstGameKill(true);
                game.setFirstGameKill(false);
            }
            Bukkit.getPluginManager().callEvent(ev);

            removeLastDamager(gamePlayer);
            getDamagers(gamePlayer).removeAllDamagers();
        } else {
            GamePlayerDeathEvent ev = new GamePlayerDeathEvent(
                    game, null, PlayerManager.getGamePlayer(player),
                    null,
                    player.getLastDamageCause() == null ? null : damageType,
                    drops);
            Bukkit.getPluginManager().callEvent(ev);
        }

        cleanupPlayer(gamePlayer);
    }

    public static List<GamePlayer> getAssists(GamePlayer gamePlayer, GamePlayer killer) {
        List<GamePlayer> assists = new ArrayList<>();
        for (GamePlayer dgp : getDamagers(gamePlayer).getDamagers().keySet()) {
            if (System.currentTimeMillis() - getDamagers(gamePlayer).getLong(dgp) <= KILL_CREDIT_TIMEOUT_MS) {
                if (!dgp.equals(killer)) assists.add(dgp);
            }
        }
        return assists;
    }

    @Getter
    public static class Damager {
        private final GamePlayer damaged;
        private final Map<GamePlayer, Long> damagers = new HashMap<>();

        public Damager(GamePlayer damaged) { this.damaged = damaged; }

        public Damager setDamager(GamePlayer damager, long ms) { damagers.put(damager, ms); return this; }
        public void removeDamager(GamePlayer damager) { damagers.remove(damager); }
        public void removeAllDamagers() { damagers.clear(); }
        public Long getLong(GamePlayer damager) { return damagers.get(damager); }
    }

    @Getter
    public static class LastDamager {
        private long ms;
        private final GamePlayer damaged;
        private GamePlayer lastDamager;

        public LastDamager(GamePlayer damaged) { this.damaged = damaged; }

        public LastDamager setLastDamager(GamePlayer lastDamager, long ms) {
            this.lastDamager = lastDamager;
            this.ms = ms;
            return this;
        }
    }
}