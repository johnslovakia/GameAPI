package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.messages.MessageType;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
public class Cooldown{

    @Getter
    private static List<Cooldown> list = new ArrayList<>();

    private final String name;
    @Setter
    private double cooldown;
    @Setter
    private Consumer<Cooldown> endConsumer;
    private final Map<GamePlayer, Double> players = new HashMap<>();

    public Cooldown(String name, double cooldown) {
        this.name = name;
        this.cooldown = cooldown;

        list.add(this);
    }

    public void startCooldown(GamePlayer gamePlayer){
        startCooldown(gamePlayer, false, null);
    }

    public void startCooldown(GamePlayer gamePlayer, boolean actionbar){
        startCooldown(gamePlayer, actionbar, null);
    }


    public void startCooldown(GamePlayer gamePlayer, boolean actionbar, Predicate<GamePlayer> actionValidation){
        players.put(gamePlayer, (double) cooldown);

        new BukkitRunnable(){
            @Override
            public void run() {
                players.put(gamePlayer, players.get(gamePlayer) - 0.1D);

                if (actionValidation == null || actionValidation.test(gamePlayer)) {
                    if (getCountdown(gamePlayer) <= 0){
                        if (endConsumer != null)
                            endConsumer.accept(Cooldown.this);
                        players.remove(gamePlayer);
                        if (actionbar) {
                            MessageManager.get(gamePlayer, "kit_ability.countdown_is_over").send(MessageType.ACTIONBAR);
                        }
                        this.cancel();
                    }else {
                        if (actionbar) {
                            Utils.countdownTimerBar(gamePlayer, name, cooldown, getCountdown(gamePlayer));
                        }
                    }
                }
            }
        }.runTaskTimer(Minigame.getInstance().getPlugin(), 0L, 2L);
    }


    public double getCountdown(GamePlayer gamePlayer){
        if (players.containsKey(gamePlayer)){
            return players.get(gamePlayer);
        }
        return 0;
    }

    public boolean contains(GamePlayer gamePlayer){
        return players.containsKey(gamePlayer);
    }


    public static Cooldown getCooldown(String name){
        for (Cooldown cd : getList()){
            if (cd.getName().equalsIgnoreCase(name)){
                return cd;
            }
        }
        return null;
    }
}
