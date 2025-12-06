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
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;

import java.util.*;

public class PVPListener implements Listener {


    //public static Map<GamePlayer, List<GamePlayer>> damagers = new HashMap<>();
    //public static Map<GamePlayer, Map<GamePlayer, Long>> lastDamager = new HashMap<>();


    //TODO: optimalizovat

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



    public Damager getDamagers(GamePlayer killed){
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
        if (game == null || game.getState() != GameState.INGAME || game.isPreparation()) {
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
        if (damageRecord == null) {
            //e.setCancelled(true);
            return;
        }
        GamePlayer damager = damageRecord.damager;

        if (damager.isSpectator() || (player.equals(damager) && !damageRecord.cause.name().toLowerCase().contains("explosion"))){
            e.setCancelled(true);
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

        if (game.getSettings().isUseTeams() &&
                player.getGameSession().getTeam().equals(damager.getGameSession().getTeam())) {
            e.setCancelled(true);
            return;
        }

        if (!player.getGameSession().isEnabledPVP()) {
            e.setCancelled(true);
            return;
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
        GameUtils.damagePlayer(victim, 0.3);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void entityDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player player) {
            GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
            GameInstance game = gamePlayer.getGame();
            if (game != null && game.getState() == GameState.INGAME) {
                if (gamePlayer.isSpectator() && e.getCause().equals(EntityDamageEvent.DamageCause.VOID)){
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
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Minigame.getInstance().getPlugin()), () -> {
            e.getEntity().spigot().respawn();
            e.getEntity().setFireTicks(0);
        }, 2L);
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath3(PlayerDeathEvent e) {
        Player player = e.getEntity();

        /*List<ItemStack> copy = new ArrayList<>(e.getDrops());
        e.getDrops().clear();
        for (ItemStack item : copy) {
            if (item.getType().equals(Material.CARVED_PUMPKIN) && item.hasItemMeta() && item.getItemMeta().hasCustomModelData())
                continue;
            player.getWorld().dropItemNaturally(player.getLocation().clone().add(0, 0.2, 0), item);
        }*/
        e.getDrops().removeIf(item ->
                item.getType() == Material.CARVED_PUMPKIN &&
                        item.hasItemMeta() &&
                        item.getItemMeta().hasCustomModelData()
        );
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);

        if (gamePlayer.getGameSession().getState().equals(GamePlayerState.DISCONNECTED)){
            e.setKeepInventory(true);
            e.setKeepLevel(true);
        }

        gamePlayer.getMetadata().put("death_location", player.getLocation());

        boolean killer = false;
        if (hasLastDamager(gamePlayer)) {
            GamePlayer last = getLastDamager(gamePlayer).getLastDamager();
            if (last != null && last != gamePlayer) {
                if (System.currentTimeMillis() - getLastDamager(gamePlayer).getMs() <= 12000){
                    killer = true;
                }
            }
        }

        DamageType damageType = e.getDamageSource().getDamageType();
        if (gamePlayer.getMetadata().containsKey("diedInVoid")){
            damageType = DamageType.OUT_OF_WORLD;
            gamePlayer.getMetadata().remove("diedInVoid");
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

            GamePlayerDeathEvent ev = new GamePlayerDeathEvent(gamePlayer.getGame(), gamePlayerKiller, PlayerManager.getGamePlayer(player), (!assists.isEmpty() ? assists : null), damageType);
            if (gamePlayer.getGame().isFirstGameKill()){
                ev.setFirstGameKill(true);
                gamePlayer.getGame().setFirstGameKill(false);
            }
            Bukkit.getPluginManager().callEvent(ev);

            removeLastDamager(gamePlayer);
            getDamagers(gamePlayer).removeAllDamagers();
        } else {
            if (player.getLastDamageCause() == null) {
                GamePlayerDeathEvent ev = new GamePlayerDeathEvent(gamePlayer.getGame(), null, PlayerManager.getGamePlayer(player), null,null);
                Bukkit.getPluginManager().callEvent(ev);
            } else {
                GamePlayerDeathEvent ev = new GamePlayerDeathEvent(gamePlayer.getGame(), null, PlayerManager.getGamePlayer(player), null, damageType);
                Bukkit.getPluginManager().callEvent(ev);
            }
        }


        lastDamager.removeIf(d -> d.getDamaged().equals(player));
        damagers.removeIf(d -> d.getDamaged().equals(player));

        e.setDeathMessage(null);
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