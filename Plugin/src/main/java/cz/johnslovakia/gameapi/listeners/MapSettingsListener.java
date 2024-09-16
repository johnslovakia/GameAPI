package cz.johnslovakia.gameapi.listeners;


import com.cryptomorin.xseries.XMaterial;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.map.AreaSettings;
import cz.johnslovakia.gameapi.game.map.AreaManager;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.ArrayList;
import java.util.List;

public class MapSettingsListener implements Listener {


    @EventHandler(priority = EventPriority.LOWEST)
    public void playerInteract(PlayerInteractEvent e){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        Game game = gamePlayer.getPlayerData().getGame();

        if (gamePlayer.isSpectator()){
            if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                if (e.getClickedBlock() != null) {
                    if (e.getClickedBlock().getType().equals(XMaterial.FURNACE.parseMaterial())
                            || e.getClickedBlock().getType().equals(XMaterial.CRAFTING_TABLE.parseMaterial())
                            || e.getClickedBlock().getType().equals(XMaterial.ENCHANTING_TABLE.parseMaterial())
                            || e.getClickedBlock().getType().equals(XMaterial.DROPPER.parseMaterial())) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }

        AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
        if(settings != null){
            if(game != null){
                if(!settings.canPlayerInteract()){
                    e.setCancelled(true);
                    return;
                }
                if(e.getAction().equals(Action.RIGHT_CLICK_BLOCK)){
                    //CHEST ACCESS
                    if(e.getClickedBlock().getType().equals(Material.CHEST) || e.getClickedBlock().getType().equals(Material.TRAPPED_CHEST) || e.getClickedBlock().getType().equals(Material.ENDER_CHEST) ){
                        if(!settings.canChestAccess()){
                            e.setCancelled(true);
                        }
                    }
                    //FLINT & STEEL
                    if(e.getItem() != null){
                        if(e.getItem().getType().equals(Material.FLINT_AND_STEEL)){
                            if(!settings.canFlintAndSteel()) e.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void explosionEvent(EntityExplodeEvent e){
        AreaSettings settings = AreaManager.getActiveSettings(e.getLocation());
        if(settings != null){
            if(e.getEntityType().equals(EntityType.CREEPER)){
                if(!settings.canCreeperExplosion()){
                    e.setCancelled(true);
                }
            }else if(e.getEntityType().equals(EntityType.FIREBALL)){
                if(!settings.canGhastFireballExplosion()) e.setCancelled(true);
            }else if(e.getEntityType().equals(EntityType.TNT)){ //TODO: nms - 1.8 PRIMED_TNT
                if(!settings.canTNTExplosion()) e.setCancelled(true);
            }else{
                if(!settings.canOtherExplosion()) e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void enderpearlEvent(ProjectileLaunchEvent e){
        if(e.getEntity().getShooter() != null){
            if(e.getEntity().getShooter() instanceof Player){
                GamePlayer gamePlayer = PlayerManager.getGamePlayer((Player) e.getEntity().getShooter());

                AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
                if(settings != null){
                    if(!AreaManager.getActiveSettings(gamePlayer).canEnderpearl()){
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void entityGrief(EntityChangeBlockEvent e){
        AreaSettings settings = AreaManager.getActiveSettings(e.getBlock().getLocation());
        if(settings != null){
            if(!settings.canEnderDragonDestroy() && e.getEntityType().equals(EntityType.ENDER_DRAGON)) e.setCancelled(true);
            if(!settings.canEndermanGrief() && e.getEntityType().equals(EntityType.ENDERMAN)) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void expDrop(EntityDeathEvent e){
        AreaSettings settings = AreaManager.getActiveSettings(e.getEntity().getLocation());
        if(settings != null){
            if(!settings.canExpDrop()){
                e.setDroppedExp(0);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void blockSpread(BlockSpreadEvent e){

        AreaSettings settings = AreaManager.getActiveSettings(e.getBlock().getLocation());
        if(settings != null){
            if(e.getNewState().getType().equals(Material.FIRE)){
                if(!settings.canFireSpread()){
                    e.setCancelled(true);
                }
            } else if(e.getNewState().getType().equals(XMaterial.GRASS_BLOCK.parseMaterial())){
                if(!settings.canGrassGrowth()) e.setCancelled(true);
            } else if(e.getNewState().getType().equals(Material.BROWN_MUSHROOM) || e.getNewState().getType().equals(Material.RED_MUSHROOM)){
                if(!settings.canMushroomGrowth()) e.setCancelled(true);
            } else if(e.getNewState().getType().equals(XMaterial.MYCELIUM.parseMaterial())){
                if(!settings.canMyceliumSpread()) e.setCancelled(true);
            } else if(e.getNewState().getType().equals(Material.VINE)){
                if(!settings.canVineGrowth()) e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void blockForm(BlockFormEvent e){
        AreaSettings settings = AreaManager.getActiveSettings(e.getBlock().getLocation());
        if(settings != null){
            if(e.getNewState().equals(Material.ICE)){
                if(!settings.canIceForm()) e.setCancelled(true);

            }else if(e.getNewState().equals(Material.SNOW)){
                if(!settings.canSnowCollection()) e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void itemDrop(PlayerDropItemEvent e){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
        if(settings != null){
            if(!settings.canItemDrop()) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void entityDamage(EntityDamageEvent e){
        if (!(e.getEntity() instanceof Player)){
            return;
        }
        Player player = (Player) e.getEntity();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        Game game = gamePlayer.getPlayerData().getGame();

        if (game != null) {
            if (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING || game.getState() == GameState.ENDING) {
                e.setCancelled(true);
                if (e.getEntity() instanceof Player) {
                    if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
                        if (game.getState() == GameState.ENDING && game.getSettings().teleportPlayersAfterEnd()) {
                            player.teleport(RespawnListener.getNonRespawnLocation(game));
                        }else{
                            e.getEntity().teleport(game.getLobbyPoint());
                        }
                    }
                }
            }
        }

        AreaSettings settings = AreaManager.getActiveSettings(e.getEntity().getLocation());
        if(settings != null){
            if (!settings.isAllowFallDamage()) {
                if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    e.setCancelled(true);
                }
            }

            if(e.getEntity().getType().equals(EntityType.ITEM_FRAME)){
                if(!settings.canItemFrameDamage()) e.setCancelled(true);
            }else if(e.getEntity().getType().equals(EntityType.PAINTING)){
                if(!settings.canPaintingDamage()) e.setCancelled(true);
            }else if(e.getEntity().getType().equals(EntityType.PLAYER)){
                AreaSettings pSettings = AreaManager.getActiveSettings(gamePlayer);
                if(pSettings != null){
                    if(pSettings.canPlayerInvincibility()) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void igniteEvent(BlockIgniteEvent e){
        AreaSettings settings = AreaManager.getActiveSettings(e.getBlock().getLocation());
        if(settings != null){
            if (e.getIgnitingBlock() == null){
                return;
            }
            if(e.getIgnitingBlock().getType().equals(XMaterial.LAVA_CAULDRON.parseMaterial()) || e.getIgnitingBlock().getType().equals(Material.LAVA)){
                if(!settings.canLavaFire()) e.setCancelled(true);
            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void fluidFlow(BlockFromToEvent e){
        AreaSettings settings = AreaManager.getActiveSettings(e.getBlock().getLocation());
        if(settings != null){
            if(e.getBlock().getType().equals(Material.LAVA)){
                if(!settings.canLavaFlow()) e.setCancelled(true);
            }else if(e.getBlock().getType().equals(Material.WATER)){
                if(!settings.canWaterFlow()) e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void leafDecay(LeavesDecayEvent e){
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void lightningStrike(LightningStrikeEvent e){
        AreaSettings settings = AreaManager.getActiveSettings(e.getLightning().getLocation());
        if(settings != null){
            if(!settings.canLightning()) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void allowMobSpawn(CreatureSpawnEvent e){
        AreaSettings settings = AreaManager.getActiveSettings(e.getEntity().getLocation());
        if(settings != null){
            if(!settings.canMobSpawn() && e.getSpawnReason().equals(SpawnReason.NATURAL)) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void pistonExtend(BlockPistonExtendEvent e){
        AreaSettings settings = AreaManager.getActiveSettings(e.getBlock().getLocation());
        if(settings != null){
            if(!settings.canPistons()) e.setCancelled(true);
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void pistonRetract(BlockPistonRetractEvent e){
        AreaSettings settings = AreaManager.getActiveSettings(e.getBlock().getLocation());
        if(settings != null){
            if(!settings.canPistons()) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void playerEnterBed(PlayerBedEnterEvent e){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());

        AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
        if(settings != null){
            if(!settings.canPlayerSleep()) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void blockMelt(BlockFadeEvent e){
        AreaSettings settings = AreaManager.getActiveSettings(e.getBlock().getLocation());
        if(settings != null){
            if(e.getBlock().getType().equals(Material.SNOW)){
                if(!settings.canSnowMelt()) e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void vehicleDestroy(VehicleDestroyEvent e){
        AreaSettings settings = AreaManager.getActiveSettings(e.getVehicle().getLocation());
        if(settings != null){
            if(!settings.canVehicleDestroy()) e.setCancelled(true);
        }
    }


    //TODO: look
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockPlace(BlockPlaceEvent e){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        Game game = gamePlayer.getPlayerData().getGame();

        AreaSettings settings = AreaManager.getActiveSettings(e.getBlockPlaced().getLocation());
        if(settings != null){
            if (!settings.isCanPlaceAll() && !settings.getCanPlace().contains(e.getBlock().getType())){
                e.setCancelled(true);
                return;
            }
            game.addBlock(e.getBlock());
        }
    }


    //TODO: look
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockBreak(BlockBreakEvent e){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        Game game = gamePlayer.getPlayerData().getGame();

        AreaSettings settings = AreaManager.getActiveSettings(e.getBlock().getLocation());
        if(settings != null){
            if (settings.isCanBreakOnlyPlacedBlocks()){
                if (!settings.getCanBreak().isEmpty() && settings.getCanBreak().containsKey(e.getBlock().getType())){
                    if (!settings.getAllowBlockDrop()){
                        e.setCancelled(true);
                        e.getBlock().setType(Material.AIR);
                    }
                    return;
                }
                if(game.containsBlock(e.getBlock())){
                    if (!settings.getAllowBlockDrop()){
                        e.setCancelled(true);
                        e.getBlock().setType(Material.AIR);
                    }
                    game.removeBlock(e.getBlock());
                    return;
                }
                e.setCancelled(true);
                return;
            }

            if (settings.isCanBreakAll()) {
                return;
            }

            if (!settings.getCanBreak().isEmpty()) {
                if (settings.getCanBreak().containsKey(e.getBlock().getType())) {
                    boolean canAlwaysBreak = (settings.getCanBreak().get(e.getBlock().getType()) != null ? settings.getCanBreak().get(e.getBlock().getType()) : false);

                    if (game.containsBlock(e.getBlock())) {
                        game.removeBlock(e.getBlock());
                    } else {
                        if (!canAlwaysBreak) {
                            e.setCancelled(true);
                            return;
                        }
                    }

                    if (!settings.getAllowBlockDrop()){
                        e.setCancelled(true);
                        e.getBlock().setType(Material.AIR);
                    }
                } else {
                    e.setCancelled(true);
                }
            }else{
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFoodchange(FoodLevelChangeEvent e){
        if (!(e.getEntity() instanceof Player)){
            return;
        }

        GamePlayer gamePlayer = PlayerManager.getGamePlayer((Player) e.getEntity());

        AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
        if(settings != null){
            if(!settings.isAllowFoodLevelChange()){
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void playerDeath(PlayerDeathEvent e){
        GamePlayer player = PlayerManager.getGamePlayer(e.getEntity());

        AreaSettings settings = AreaManager.getActiveSettings(player);
        if(settings != null){
            if(settings.isKeepInventory()){
                e.setKeepInventory(true);
            }
            e.setDeathMessage("");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemDurabilityChange(PlayerItemDamageEvent e){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
        if(settings != null){
            e.setCancelled(!settings.isAllowDurabilityChange());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        GamePlayer player = PlayerManager.getGamePlayer((Player) e.getWhoClicked());

        if (e.getView().getTitle().equalsIgnoreCase("Inventory Editor")){
            return;
        }

        AreaSettings settings = AreaManager.getActiveSettings(player);
        if(settings != null){
            if(!settings.isAllowInventoryChange()) e.setCancelled(true);
        }
    }


    /*@EventHandler
    public void onLogin(PlayerLoginEvent e) {
        if (e.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return;
        }
        if (GameManager.getGame() == null){
            return;
        }

        if (GameManager.getGame().getState() == GameState.RESTARTING) {
            e.getPlayer().sendMessage(Prefix.LOCAL_PREFIX+ "§cThis game is restarting!");
            GameUtil.newArena(e.getPlayer());
            //e.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§cThis arena is restarting!");
            return;
        }else if (GameManager.getGame().getState() == GameState.ENDING) {
            e.getPlayer().sendMessage(Prefix.LOCAL_PREFIX+ "§cThis game is ending!");
            GameUtil.newArena(e.getPlayer());
            //e.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§cThis arena is ending!");
            return;
        }else if (GameManager.getGame().getState() == GameState.PREPARATION) {
            e.getPlayer().sendMessage(Prefix.LOCAL_PREFIX+ "§cThis game is in Preparation state!");
            GameUtil.newArena(e.getPlayer());
            //e.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§cThis arena is in Preparation state!");
            return;
        }else if ((GameManager.getGame().getState() == GameState.LOBBY) && GameManager.getGame().getPlayers().size() >= GameManager.getGame().getSettings().getMaxPlayers()) {
            e.getPlayer().sendMessage(Prefix.LOCAL_PREFIX+ "§cThis game is full!");
            GameUtil.newArena(e.getPlayer());
            //e.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§cThis arena is full!");
            return;
        }else{
            e.getPlayer().sendMessage(Prefix.LOCAL_PREFIX+ "§cThis game is loading!");
            GameUtil.newArena(e.getPlayer());
        }
        /*if (GameManager.getGame().getState() == GameState.INGAME && (!GameManager.getGame().getSettings().isAllowedJoiningAfterStart() || !GameManager.getGame().getSettings().isEnabledReJoin())) {
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§cYou can not connect to arena!");
        }*

        //TODO: Zkontrolovat
    }*/





    @EventHandler(priority = EventPriority.HIGH)
    public void playerDamagePlayer(EntityDamageByEntityEvent e){
        if (e.getDamager() instanceof Player){
            GamePlayer damager = PlayerManager.getGamePlayer((Player) e.getDamager());
            if (damager.isSpectator()){
                e.setCancelled(true);
                return;
            }
        }



        if(Utils.isPlayerDamager(e) && e.getEntity() instanceof Player){
            GamePlayer damaged = PlayerManager.getGamePlayer((Player) e.getEntity());
            GamePlayer damager = Utils.getDamager(e);
            Game damagedGame = damaged.getPlayerData().getGame();
            Game damagerGame = damager.getPlayerData().getGame();;

            if (damager.isSpectator()){
                e.setCancelled(true);
                return;
            }
            if(damagerGame != null){
                AreaSettings settings = AreaManager.getActiveSettings(damager);
                if(settings != null){
                    if(!settings.isCanPvP()){
                        e.setCancelled(true);
                    }else{
                        if(damagedGame != null){
                            if(damagedGame.equals(damagerGame)){
                                if(damaged.getPlayerData().getTeam() != null && damager.getPlayerData().getTeam() != null && damaged.getPlayerData().getTeam().equals(damager.getPlayerData().getTeam())){
                                    e.setCancelled(true);
                                }
                            }else{
                                e.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }
    }


    @EventHandler
    public void onWeatherChange(WeatherChangeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        Player player = e.getPlayer();
        Game game = PlayerManager.getGamePlayer(player).getPlayerData().getGame();

        if (game != null){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            GamePlayer gp = PlayerManager.getGamePlayer(player);
            Game game = PlayerManager.getGamePlayer(player).getPlayerData().getGame();

            if (game != null && game.getState() != GameState.INGAME) {
                e.setCancelled(true);
            }else if (game != null){
                if (gp.isSpectator()){
                    e.setCancelled(true);
                }
            }
        }
    }


    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent e) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());

        AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
        if(settings != null){
            if(!settings.isAllowItemPicking()) e.setCancelled(true);
        }
    }


    /*@EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        GamePlayer gp = PlayerManager.getGamePlayer(player);
        Game game = gp.getGame();

        if (game != null){

            if (game.getState() != GameState.INGAME){
                e.setCancelled(true);
            }else if (game.getState() == GameState.INGAME){
                if (gp.isSpectator()){
                    e.setCancelled(true);
                }
            }
        }else{
            if (GameAPI.isSetup()) {
                e.setCancelled(true);
            }
        }
    }*/


    List<Player> noFallDamage = new ArrayList<>();


    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        GamePlayer gp = PlayerManager.getGamePlayer(player);

        AreaSettings settings = AreaManager.getActiveSettings(gp);
        if (settings != null) {
            if (e.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
                if (!settings.isAllowEnderpearlFallDamage()) {
                    player.setNoDamageTicks(2);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerLaunchProjectile(ProjectileLaunchEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player)){
            return;
        }

        Player player = (Player) e.getEntity().getShooter();
        GamePlayer gp = PlayerManager.getGamePlayer(player);

        AreaSettings settings = AreaManager.getActiveSettings(gp);
        if (settings != null) {
            if (!settings.isAllowEnderpearlFallDamage()) {
                if (!e.getEntity().getType().equals(EntityType.ENDER_PEARL)){
                    return;
                }
                if (!noFallDamage.contains((Player) e.getEntity().getShooter())){
                    noFallDamage.add((Player) e.getEntity().getShooter());
                }
            }
        }
    }

    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)){
            return;
        }
        Player player = (Player) event.getEntity();

        if (event.getCause().equals(EntityDamageEvent.DamageCause.FALL)
                && noFallDamage.contains(player)) {
            event.setCancelled(true);
            noFallDamage.remove(player);
        }
    }
}