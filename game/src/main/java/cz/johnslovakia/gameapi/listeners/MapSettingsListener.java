package cz.johnslovakia.gameapi.listeners;

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.inventoryBuilder.InventoryBuilder;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.lobby.LobbyModule;
import cz.johnslovakia.gameapi.modules.game.map.Area;
import cz.johnslovakia.gameapi.modules.game.map.AreaManager;
import cz.johnslovakia.gameapi.modules.game.map.AreaSettings;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;

import cz.johnslovakia.gameapi.utils.GameUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.ArrayList;
import java.util.List;

public class MapSettingsListener implements Listener {


    @EventHandler
    public void onXpPickup(PlayerPickupExperienceEvent e) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        if (!gamePlayer.isInGame()) return;

        if (gamePlayer.isSpectator() || gamePlayer.getGame().getState() != GameState.INGAME){
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void playerInteract(PlayerInteractEvent e){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        if (!gamePlayer.isInGame()) return;
        GameInstance game = gamePlayer.getGame();

        if (gamePlayer.isSpectator()){
            if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                if (e.getClickedBlock() != null) {
                    if (e.getClickedBlock().getType().equals(Material.FURNACE)
                            || e.getClickedBlock().getType().equals(Material.CRAFTING_TABLE)
                            || e.getClickedBlock().getType().equals(Material.ENCHANTING_TABLE)
                            || e.getClickedBlock().getType().equals(Material.DROPPER)) {
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
            }else if(e.getEntityType().equals(EntityType.TNT)){
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
            } else if(e.getNewState().getType().equals(Material.GRASS_BLOCK)){
                if(!settings.canGrassGrowth()) e.setCancelled(true);
            } else if(e.getNewState().getType().equals(Material.BROWN_MUSHROOM) || e.getNewState().getType().equals(Material.RED_MUSHROOM)){
                if(!settings.canMushroomGrowth()) e.setCancelled(true);
            } else if(e.getNewState().getType().equals(Material.MYCELIUM)){
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
        if (!gamePlayer.isInGame()) return;

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
        if (!gamePlayer.isInGame()) return;
        GameInstance game = gamePlayer.getGame();

        if (game != null) {
            if (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING || game.getState() == GameState.ENDING) {
                e.setCancelled(true);
                if (e.getEntity() instanceof Player) {
                    if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
                        if (game.getState() == GameState.ENDING && !game.getSettings().isTeleportPlayersAfterEnd()) {
                            player.teleport(GameUtils.getNonRespawnLocation(game));
                        }else{
                            e.getEntity().teleport(game.getModule(LobbyModule.class).getLobbyLocation().getLocation());
                        }
                    }
                }
            }
        }

        AreaSettings settings = AreaManager.getActiveSettings(e.getEntity().getLocation());
        if(settings != null){

            if(e.getEntity().getType().equals(EntityType.ITEM_FRAME)){
                if(!settings.canItemFrameDamage()) e.setCancelled(true);
            }else if(e.getEntity().getType().equals(EntityType.PAINTING)){
                if(!settings.canPaintingDamage()) e.setCancelled(true);
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
            if(e.getIgnitingBlock().getType().equals(Material.LAVA_CAULDRON) || e.getIgnitingBlock().getType().equals(Material.LAVA)){
                if(!settings.canLavaFire()) e.setCancelled(true);
            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void fluidFlow(BlockFromToEvent e){
        AreaSettings settings = AreaManager.getActiveSettings(e.getBlock().getLocation());
        if(settings != null){
            Block block = e.getBlock();
            if(block.getType().equals(Material.LAVA) || block.getBlockData().getMaterial().equals(Material.LAVA)){
                if(!settings.canLavaFlow()) e.setCancelled(true);
            }else if(block.getType().equals(Material.WATER) || block.getBlockData().getMaterial().equals(Material.WATER)){
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
        if (!gamePlayer.isInGame()) return;

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


    @EventHandler(priority = EventPriority.LOWEST)
    public void blockPlace(BlockPlaceEvent e){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        if (!gamePlayer.isInGame()) return;
        GameInstance game = gamePlayer.getGame();

        AreaSettings settings = AreaManager.getActiveSettings(e.getBlockPlaced().getLocation());
        if(settings != null){
            if (!settings.isCanPlaceAll() && !settings.getCanPlace().contains(e.getBlock().getType())){
                e.setCancelled(true);
                return;
            }
            game.addBlock(e.getBlock());
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void blockBreak(BlockBreakEvent e){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        if (!gamePlayer.isInGame()) return;
        GameInstance game = gamePlayer.getGame();

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
        if (!gamePlayer.isInGame()) return;

        AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
        if(settings != null){
            if(!settings.isAllowFoodLevelChange()){
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void playerDeath(PlayerDeathEvent e){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getEntity());
        if (!gamePlayer.isInGame()) return;

        AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
        if(settings != null){
            if(settings.isKeepInventory()){
                e.setKeepInventory(true);
            }else if (!settings.isDropItemsOnDeath()){
                e.getDrops().clear();
            }
            e.setDeathMessage("");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemDurabilityChange(PlayerItemDamageEvent e){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        if (!gamePlayer.isInGame()) return;
        AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
        if(settings != null){
            e.setCancelled(!settings.isAllowDurabilityChange());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        GamePlayer gamePlayer = PlayerManager.getGamePlayer((Player) e.getWhoClicked());
        if (!gamePlayer.isInGame()) return;

        if (e.getView().getTitle().equalsIgnoreCase("Inventory Editor") || PlainTextComponentSerializer.plainText().serialize(e.getView().title()).contains("ㆾ") || (InventoryBuilder.getPlayerCurrentInventory(gamePlayer) != null && InventoryBuilder.getPlayerCurrentInventory(gamePlayer).getName().equalsIgnoreCase("Set Kit Inventory"))){
            return;
        }

        AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
        if(settings != null){
            if(!settings.isAllowInventoryChange()) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());

        if (!gamePlayer.getGame().getState().equals(GameState.INGAME)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void playerDamagePlayer(EntityDamageByEntityEvent e){
        if (e.getDamager() instanceof Player) {
            GamePlayer damager = PlayerManager.getGamePlayer((Player) e.getDamager());
            if (!damager.isInGame()) return;
            GameInstance game = damager.getGame();
            ;
            if (damager.isSpectator()) {
                e.setCancelled(true);
                return;
            }
            if (game.getState() != GameState.INGAME){
                e.setCancelled(true);
            }
        }



        if(e.getDamager() instanceof Player && e.getEntity() instanceof Player){
            GamePlayer damaged = PlayerManager.getGamePlayer((Player) e.getEntity());
            GamePlayer damager = PlayerManager.getGamePlayer((Player) e.getDamager());
            GameInstance damagedGame = damaged.getGame();
            GameInstance damagerGame = damager.getGame();;

            if (damager.isSpectator()){
                e.setCancelled(true);
                return;
            }
            if(damagerGame != null){
                AreaSettings settings = AreaManager.getActiveSettings(damager);
                if(settings != null){
                    if(!settings.isCanPvP()){
                        e.setCancelled(true);
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
        GameInstance game = PlayerManager.getGamePlayer(player).getGame();

        if (game != null){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player player) {
            GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
            if (!gamePlayer.isInGame()) return;
            GameInstance game = PlayerManager.getGamePlayer(player).getGame();

            if ((game != null && game.getState() != GameState.INGAME) || gamePlayer.isSpectator()) {
                e.setCancelled(true);
            }

            AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
            if(settings != null){
                if (!settings.isAllowFallDamage() && e.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
                    e.setCancelled(true);
                }
                if (settings.isAllowedInstantVoidKill() && e.getCause().equals(EntityDamageEvent.DamageCause.VOID)){
                    e.setCancelled(true);
                    gamePlayer.getMetadata().put("diedInVoid", true);
                    player.damage(player.getHealth());

                }
                if(settings.canPlayerInvincibility()) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        if (!gamePlayer.isInGame()) return;
        GameInstance game = PlayerManager.getGamePlayer(player).getGame();

        if (game == null || game.getState() != GameState.INGAME) return;

        AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
        if(game.getCurrentMap().getMainArea() != null && settings != null) {
            Area borderArea =  game.getCurrentMap().getMainArea();
            if (settings.isAllowedInstantVoidKill() && borderArea != null || gamePlayer.isSpectator()) {
                double lowestAreaY = borderArea.getLocation1().getY();
                if (borderArea.getLocation2().getY() < lowestAreaY){
                    lowestAreaY = borderArea.getLocation2().getY();
                }
                if (e.getTo().getY() < lowestAreaY - 20){
                    if (!gamePlayer.isSpectator()) {
                        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                            player.getInventory().clear();
                            gamePlayer.getMetadata().put("diedInVoid", true);
                            player.damage(player.getHealth());
                        }, 1L);
                    }else{
                        player.teleport(GameUtils.getNonRespawnLocation(game));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player player) {
            GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
            if (!gamePlayer.isInGame()) return;

            AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
            if (settings != null) {
                if (!settings.isAllowItemPicking()) e.setCancelled(true);
            }
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
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        if (!gamePlayer.isInGame()) return;

        AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
        if (settings != null) {
            if (e.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
                if (!settings.isAllowEnderpearlFallDamage()) {
                    player.setNoDamageTicks(2);
                }
                if (noFallDamage.contains(player)){
                    noFallDamage.remove(player);
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
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        if (!gamePlayer.isInGame()) return;

        AreaSettings settings = AreaManager.getActiveSettings(gamePlayer);
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