package cz.johnslovakia.gameapi.game.map;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
public class AreaSettings implements Cloneable{

    @Setter
    @Getter
    private boolean canBreakOnlyPlacedBlocks = false;
    private boolean canPlaceAll = true;
    private boolean canBreakAll = true;
    @Getter
    private List<Material> canPlace = new ArrayList<>();
    @Getter
    private Map<Material, Boolean> canBreak = new HashMap<>();;
    private boolean canPvP = true;
    private boolean allowMobDamage = true;
    private boolean allowItemFrameDamage = true;
    private boolean allowPaintingDamage = true;
    private boolean allowExpDrop = true;
    private boolean allowItemDrop = true;
    private boolean allowMobSpawn = false;
    private boolean allowCreeperExplosion = true;
    private boolean allowOtherExplosion = true;
    private boolean allowEndermanGrief = true;
    private boolean allowEnderpearl = true;
    private boolean allowEnderpearlFallDamage = false;
    private boolean allowEnderDragonDestroy = true;
    private boolean allowGhastFireballExplosion = true;
    private boolean allowPlayerSleep = true;
    private boolean allowTNTExplosion = true;
    private boolean allowFlintAndSteel = true;
    private boolean allowFireSpread = true;
    private boolean allowLavaFire = true;
    private boolean allowLightning = true;
    private boolean allowChestAccess = true;
    private boolean allowPistons = true;
    private boolean allowWaterFlow = true;
    private boolean allowLavaFlow = true;
    private boolean allowPlayerInteract = true;
    private boolean allowVehiclePlacement = true;
    private boolean allowVehicleDestroy = true;
    private boolean allowSnowCollection = true;
    private boolean allowSnowMelt = true;
    private boolean allowIceForm = true;
    private boolean allowMushroomGrowth = true;
    private boolean allowLeafDecay = true;
    private boolean allowGrassGrowth = true;
    private boolean allowMyceliumSpread = true;
    private boolean allowVineGrowth = true;
    private boolean allowPlayerInvincibility = false;
    private boolean allowFoodLevelChange = true;
    private boolean keepInventory = false;
    private boolean allowDurabilityChange = true;
    private boolean allowBlockDrop = true;
    private boolean allowTimeChange = false;
    private boolean allowWeatherChange = true;
    private boolean allowInventoryChange = true;
    private boolean allowChat = true;
    private boolean allowEggHatching = false;
    @Setter
    @Getter
    private boolean allowFallDamage = true;
    private boolean allowItemPicking = true;
    private boolean loadWorldWithGameAPI = true;
    private boolean allowInstantVoidKill = true;

    /**
     * -- SETTER --
     *  Set the priority these settings take. Higher number = higher priority
     *  Default: 0 for Areas, -1 for Arenas.
     *
     *
     * -- GETTER --
     *  Get the priority these settings take. Higher number = higher priority.
     *  Default: 0 for Areas, -1 for Arenas.
     *
     @param priority The ArenaSettings' priority to be set.
      * @return This ArenaSettings' priority.
     */
    @Getter
    @Setter
    private int priority;

    public AreaSettings(){
    }

    public AreaSettings(int priority){
        this.priority = priority;
    }

    /**
     * Get the priority these settings take. Higher number = higher priority.
     * Default: 0 for Areas, -1 for Arenas.
     * @return This ArenaSettings' priority.
     */
    public int getPriority(){
        return priority;
    }

    /**
     * Set the priority these settings take. Higher number = higher priority
     * Default: 0 for Areas, -1 for Arenas.
     * @param priority The ArenaSettings' priority to be set.
     */
    public void setPriority(int priority){
        this.priority = priority;
    }


    public boolean isAllowedInstantVoidKill() {
        return allowInstantVoidKill;
    }

    public AreaSettings setAllowInstantVoidKill(boolean allowInstantVoidKill) {
        this.allowInstantVoidKill = allowInstantVoidKill;
        return this;
    }

    public void addCanPlaceBlock(Material material){
        if (canPlace.contains(material)){
            return;
        }
        canPlace.add(material);
    }

    public void addCanBreakBlock(Material... materials){
        addCanBreakBlock(false, materials);
    }
    public void addCanBreakBlock(Boolean always, Material... materials){
        for (Material material : materials) {
            if (canBreak.containsKey(material)) {
                return;
            }
            canBreak.put(material, always);
        }
    }

    public boolean allowChat(){
        return this.allowChat;
    }

    public void allowChat(boolean allow){
        this.allowChat = allow;
    }

    public boolean isAllowEggHatching() {
        return allowEggHatching;
    }

    public void setAllowEggHatching(boolean allowEggHatching) {
        this.allowEggHatching = allowEggHatching;
    }

    public boolean getAllowBlockDrop(){
        return this.allowBlockDrop;
    }

    public void setAllowBlockDrop(boolean allow){
        this.allowBlockDrop = allow;
    }

    public boolean isCanPvP() {
        return canPvP;
    }

    public void setCanPvP(boolean canPvP) {
        this.canPvP = canPvP;
    }

    public boolean canMobDamage() {
        return allowMobDamage;
    }

    public void setAllowMobDamage(boolean allowMobDamage) {
        this.allowMobDamage = allowMobDamage;
    }

    public boolean canItemFrameDamage() {
        return allowItemFrameDamage;
    }

    public void setAllowItemFrameDamage(boolean allowItemFrameDamage) {
        this.allowItemFrameDamage = allowItemFrameDamage;
    }

    public boolean canPaintingDamage() {
        return allowPaintingDamage;
    }

    public void setAllowPaintingDamage(boolean allowPaintingDamage) {
        this.allowPaintingDamage = allowPaintingDamage;
    }

    public boolean canExpDrop() {
        return allowExpDrop;
    }

    public void setAllowExpDrop(boolean allowExpDrop) {
        this.allowExpDrop = allowExpDrop;
    }

    public boolean canItemDrop() {
        return allowItemDrop;
    }

    public void setAllowItemDrop(boolean allowItemDrop) {
        this.allowItemDrop = allowItemDrop;
    }

    public boolean canMobSpawn() {
        return allowMobSpawn;
    }

    public void setAllowMobSpawn(boolean allowMobSpawn) {
        this.allowMobSpawn = allowMobSpawn;
    }

    public boolean canCreeperExplosion() {
        return allowCreeperExplosion;
    }

    public void setAllowCreeperExplosion(boolean allowCreeperExplosion) {
        this.allowCreeperExplosion = allowCreeperExplosion;
    }

    public boolean canOtherExplosion() {
        return allowOtherExplosion;
    }

    public void setAllowOtherExplosion(boolean allowOtherExplosion) {
        this.allowOtherExplosion = allowOtherExplosion;
    }

    public boolean canEndermanGrief() {
        return allowEndermanGrief;
    }

    public void setAllowEndermanGrief(boolean allowEndermanGrief) {
        this.allowEndermanGrief = allowEndermanGrief;
    }

    public boolean canEnderpearl() {
        return allowEnderpearl;
    }

    public void setAllowEnderpearl(boolean allowEndepearl) {
        this.allowEnderpearl = allowEndepearl;
    }

    public boolean canEnderDragonDestroy() {
        return allowEnderDragonDestroy;
    }

    public void setAllowEnderDragonDestroy(boolean allowEnderDragonDestroy) {
        this.allowEnderDragonDestroy = allowEnderDragonDestroy;
    }

    public boolean canGhastFireballExplosion() {
        return allowGhastFireballExplosion;
    }

    public void setAllowGhastFireballExplosion(boolean allowGhastFireballExplosion) {
        this.allowGhastFireballExplosion = allowGhastFireballExplosion;
    }

    public boolean canPlayerSleep() {
        return allowPlayerSleep;
    }

    public void setAllowPlayerSleep(boolean allowPlayerSleep) {
        this.allowPlayerSleep = allowPlayerSleep;
    }

    public boolean canTNTExplosion() {
        return allowTNTExplosion;
    }

    public void setAllowTNTExplosion(boolean allowTNTExplosion) {
        this.allowTNTExplosion = allowTNTExplosion;
    }

    public boolean canFlintAndSteel() {
        return allowFlintAndSteel;
    }

    public void setAllowFlintAndSteel(boolean allowFlintAndSteel) {
        this.allowFlintAndSteel = allowFlintAndSteel;
    }

    public boolean canFireSpread() {
        return allowFireSpread;
    }

    public void setAllowFireSpread(boolean allowFireSpread) {
        this.allowFireSpread = allowFireSpread;
    }

    public boolean canLavaFire() {
        return allowLavaFire;
    }

    public void setAllowLavaFire(boolean allowLavaFire) {
        this.allowLavaFire = allowLavaFire;
    }

    public boolean canLightning() {
        return allowLightning;
    }

    public void setAllowLightning(boolean allowLightning) {
        this.allowLightning = allowLightning;
    }

    public boolean canChestAccess() {
        return allowChestAccess;
    }

    public void setAllowChestAccess(boolean allowChestAccess) {
        this.allowChestAccess = allowChestAccess;
    }

    public boolean canPistons() {
        return allowPistons;
    }

    public void setAllowPistons(boolean allowPistons) {
        this.allowPistons = allowPistons;
    }

    public boolean canWaterFlow() {
        return allowWaterFlow;
    }

    public void setAllowWaterFlow(boolean allowWaterFlow) {
        this.allowWaterFlow = allowWaterFlow;
    }

    public boolean canLavaFlow() {
        return allowLavaFlow;
    }

    public void setAllowLavaFlow(boolean allowLavaFlow) {
        this.allowLavaFlow = allowLavaFlow;
    }

    public boolean canPlayerInteract() {
        return allowPlayerInteract;
    }

    public void setAllowPlayerInteract(boolean allowPlayerInteract) {
        this.allowPlayerInteract = allowPlayerInteract;
    }

    public boolean canVehiclePlacement() {
        return allowVehiclePlacement;
    }

    public void setAllowVehiclePlacement(boolean allowVehiclePlacement) {
        this.allowVehiclePlacement = allowVehiclePlacement;
    }

    public boolean canVehicleDestroy() {
        return allowVehicleDestroy;
    }

    public void setAllowVehicleDestroy(boolean allowVehicleDestroy) {
        this.allowVehicleDestroy = allowVehicleDestroy;
    }

    public boolean canSnowCollection() {
        return allowSnowCollection;
    }

    public void setAllowSnowCollection(boolean allowSnowCollection) {
        this.allowSnowCollection = allowSnowCollection;
    }

    public boolean canSnowMelt() {
        return allowSnowMelt;
    }

    public void setAllowSnowMelt(boolean allowSnowMelt) {
        this.allowSnowMelt = allowSnowMelt;
    }

    public boolean canIceForm() {
        return allowIceForm;
    }

    public void setAllowIceForm(boolean allowIceForm) {
        this.allowIceForm = allowIceForm;
    }

    public boolean canMushroomGrowth() {
        return allowMushroomGrowth;
    }

    public void setAllowMushroomGrowth(boolean allowMushroomGrowth) {
        this.allowMushroomGrowth = allowMushroomGrowth;
    }

    public boolean canLeafDecay() {
        return allowLeafDecay;
    }

    public void setAllowLeafDecay(boolean allowLeafDecay) {
        this.allowLeafDecay = allowLeafDecay;
    }

    public boolean canGrassGrowth() {
        return allowGrassGrowth;
    }

    public void setAllowGrassGrowth(boolean allowGrassGrowth) {
        this.allowGrassGrowth = allowGrassGrowth;
    }

    public boolean canMyceliumSpread() {
        return allowMyceliumSpread;
    }

    public void setAllowMyceliumSpread(boolean allowMyceliumSpread) {
        this.allowMyceliumSpread = allowMyceliumSpread;
    }

    public boolean canVineGrowth() {
        return allowVineGrowth;
    }

    public void setAllowVineGrowth(boolean allowVineGrowth) {
        this.allowVineGrowth = allowVineGrowth;
    }

    public boolean canPlayerInvincibility() {
        return allowPlayerInvincibility;
    }

    public void setAllowPlayerInvincibility(boolean allowPlayerInvincibility) {
        this.allowPlayerInvincibility = allowPlayerInvincibility;
    }

    public boolean isAllowFoodLevelChange() {
        return allowFoodLevelChange;
    }

    public void setAllowFoodLevelChange(boolean allowFoodLevelChange) {
        this.allowFoodLevelChange = allowFoodLevelChange;
    }

    public boolean isKeepInventory() {
        return keepInventory;
    }

    public void setKeepInventory(boolean keepInventory) {
        this.keepInventory = keepInventory;
    }

    public boolean isAllowDurabilityChange() {
        return allowDurabilityChange;
    }

    public void setAllowDurabilityChange(boolean allowDurabilityChange) {
        this.allowDurabilityChange = allowDurabilityChange;
    }

    public boolean isAllowTimeChange() {
        return allowTimeChange;
    }

    public void setAllowTimeChange(boolean allowTimeChange) {
        this.allowTimeChange = allowTimeChange;
    }

    public boolean isAllowWeatherChange() {
        return allowWeatherChange;
    }

    public void setAllowWeatherChange(boolean allowWeatherChange) {
        this.allowWeatherChange = allowWeatherChange;
    }

    public boolean isAllowInventoryChange() {
        return allowInventoryChange;
    }

    public void setAllowInventoryChange(boolean allowInventoryChange) {
        this.allowInventoryChange = allowInventoryChange;
    }

    public boolean isCanPlaceAll() {
        return canPlaceAll;
    }

    public void setCanPlaceAll(boolean canPlaceAll) {
        this.canPlaceAll = canPlaceAll;
    }

    public boolean isCanBreakAll() {
        return canBreakAll;
    }

    public void setCanBreakAll(boolean canBreakAll) {
        this.canBreakAll = canBreakAll;
    }

    public boolean isAllowEnderpearlFallDamage() {
        return allowEnderpearlFallDamage;
    }

    public void setAllowEnderpearlFallDamage(boolean allowEnderpearlFallDamage) {
        this.allowEnderpearlFallDamage = allowEnderpearlFallDamage;
    }

    public boolean isAllowItemPicking() {
        return allowItemPicking;
    }

    public void setAllowItemPicking(boolean allowItemPicking) {
        this.allowItemPicking = allowItemPicking;
    }

    public boolean isLoadWorldWithGameAPI() {
        return loadWorldWithGameAPI;
    }

    public void setLoadWorldWithGameAPI(boolean loadWorldWithGameAPI) {
        this.loadWorldWithGameAPI = loadWorldWithGameAPI;
    }

    @Override
    public AreaSettings clone() {
        try {
            AreaSettings clone = (AreaSettings) super.clone();

            clone.canPlace = new ArrayList<>(this.canPlace);
            clone.canBreak = new HashMap<>(this.canBreak);

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}