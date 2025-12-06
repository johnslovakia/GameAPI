package cz.johnslovakia.gameapi.modules.perks;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.PlayerIdentity;

import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.eTrigger.Condition;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Perk implements Listener{

    private final String name;
    private final ItemStack icon;
    private final PerkType type;

    private List<PerkLevel> levels = new ArrayList<>();
    private Set<Trigger<?>> triggers = new HashSet<>();

    public Perk(String name, ItemStack icon, PerkType type) {
        this.name = name;
        this.icon = icon;
        this.type = type;
        Bukkit.getPluginManager().registerEvents(this, Minigame.getInstance().getPlugin());
    }

    public Perk(String name, ItemStack icon, PerkType type, List<PerkLevel> levels) {
        this.name = name;
        this.icon = icon;
        this.type = type;
        this.levels = levels;
        Bukkit.getPluginManager().registerEvents(this, Minigame.getInstance().getPlugin());
    }

    public void addLevel(PerkLevel perkLevel){
        if (!levels.contains(perkLevel)){
            levels.add(perkLevel);
        }
    }

    public void addTrigger(Trigger<?> trigger){
        triggers.add(trigger);
        Minigame.getInstance().getPlugin().getServer().getPluginManager().registerEvent(trigger.getEventClass(), new Listener() { }, EventPriority.NORMAL, (listener, event) -> onEventCall(event), Minigame.getInstance().getPlugin());
    }

    public void purchase(GamePlayer gamePlayer) {
        Player player = gamePlayer.getOnlinePlayer();

        ResourcesModule resourcesModule = ModuleManager.getModule(ResourcesModule.class);
        Resource resource = Minigame.getInstance().getPerkManager().getResource();
        PerkLevel nextLevel = Minigame.getInstance().getPerkManager().getNextPlayerPerkLevel(gamePlayer, this);

        resourcesModule.getPlayerBalance(gamePlayer, resource).thenAccept(balance -> {
            if (nextLevel != null){
                Integer nextLevelPrice = nextLevel.price();

                if (balance >= nextLevelPrice){
                    gamePlayer.getPlayerData().setPerkLevel(this, nextLevel.level());

                    ModuleManager.getModule(MessageModule.class).get(player, "chat.perk.purchase")
                            .replace("%economy_name%", resource.getDisplayName())
                            .replace("%price%", "" + nextLevelPrice)
                            .replace("%perk%", getName() + " " + StringUtils.numeral(nextLevel.level()))
                            .send();
                    gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "jsplugins:purchase", 1F, 1.0F);
                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            resourcesModule.withdraw(gamePlayer, resource, nextLevelPrice);
                        }
                    }.runTaskAsynchronously(Minigame.getInstance().getPlugin());
                }else{
                    ModuleManager.getModule(MessageModule.class).get(player, "chat.dont_have_enough")
                            .replace("%economy_name%", resource.getDisplayName())
                            .replace("%need_more%", "" + (nextLevelPrice - balance))
                            .send();
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 10.0F, 10.0F);
                }
            }else{
                ModuleManager.getModule(MessageModule.class).get(player, "chat.perk.max_level")
                        .send();
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 10.0F, 10.0F);
            }
        });
    }


    private boolean checkConditions(PlayerIdentity target) {
        boolean result = true;
        boolean alternativeResult = false;

        for(Method method : getClass().getDeclaredMethods()){

            if(!method.isAnnotationPresent(Condition.class)) continue;
            if(!method.getReturnType().equals(boolean.class))
                if(method.getParameterCount() > 1) continue;
            method.setAccessible(true);

            Condition condition = method.getAnnotation(Condition.class);

            boolean invokeResult = false;
            try {
                if (method.getParameterCount() == 0) {
                    invokeResult = (boolean) method.invoke(this);
                } else {
                    invokeResult = (boolean) method.invoke(this, target);
                }
            } catch (InvocationTargetException | IllegalAccessException ignored) {
            }

            if(!condition.alternative()) {
                result = (invokeResult == !condition.negate()) && result;
            } else {
                alternativeResult = (invokeResult && !condition.negate()) || alternativeResult;
            }
        }
        return result || alternativeResult;
    }

    private void onEventCall(Event event){
        for (Trigger<?> trigger : this.getTriggers()) {
            Class<? extends Event> clazz = trigger.getEventClass();
            if (clazz.equals(event.getClass())) {
                if (!trigger.validate(clazz.cast(event))) continue;
                //GamePlayer gamePlayer = trigger.compute(clazz.cast(event));
                for (PlayerIdentity playerIdentity : trigger.compute(clazz.cast(event))) {
                    if (checkConditions(playerIdentity)) {
                        trigger.getResponse().accept(playerIdentity);
                        return;
                    }
                }
            }
        }
    }


    public String getTranslationKey(){
        return "perk." + getName().toLowerCase().replace(" ", "_");
    }
}
