package cz.johnslovakia.gameapi.economy;

import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerScore;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Economy {

    public static List<Economy> economyList = new ArrayList<>();


    @Getter
    private String name, img_char;
    @Getter
    private ChatColor chatColor;
    @Getter
    private int rank;

    @Getter @Setter
    private EconomyInterface economyInterface;


    /**
     * To make the economy work, set the economy using setOnAdd(), setOnRemove() and setBallanceRunnable()
     * or by using setEconomyAutomatically() method this will be set itself and link it to the database
     * tables created by GameAPI.
     *
     * @param rank In case of multiple economies. Set the order in which Economy rewards
     *             are listed at the end of the game.
     */
    public Economy(String name, ChatColor chatColor, int rank, EconomyInterface economyInterface) {
        this.name = name;
        this.chatColor = chatColor;
        this.rank = rank;
        this.economyInterface = economyInterface;

        economyList.add(this);
    }

    public Economy(String name, String img_char, ChatColor chatColor, int rank, EconomyInterface economyInterface) {
        this.name = name;
        this.img_char = img_char;
        this.chatColor = chatColor;
        this.rank = rank;
        this.economyInterface = economyInterface;

        economyList.add(this);
    }

    public Economy(String name, ChatColor chatColor, int rank, boolean automatically, boolean forAllMinigames) {
        this.name = name;
        this.chatColor = chatColor;
        this.rank = rank;
        this.automatically = automatically;
        this.forAllMinigames = forAllMinigames;

        economyList.add(this);
    }

    public Economy(String name, String img_char, ChatColor chatColor, int rank, boolean automatically, boolean forAllMinigames) {
        this.name = name;
        this.img_char = img_char;
        this.chatColor = chatColor;
        this.rank = rank;
        this.automatically = automatically;
        this.forAllMinigames = forAllMinigames;

        economyList.add(this);
    }

    @Getter
    private boolean automatically, forAllMinigames;

    /**
     * @param forAllMinigames If is set to true, all minigames will have a linked economy.
     *                        If is set to false, the Minigame Table must be assigned before executing this method.
     */
    public void setEconomyAutomatically(boolean automatically, boolean forAllMinigames){
        this.automatically = automatically;
        this.forAllMinigames = forAllMinigames;
    }

    public String formattedName() {
        String caps = name.toLowerCase();
        return caps.substring(0, 1).toUpperCase() + caps.substring(1);
    }

    public static List<Economy> getEconomies() {
        return economyList;
    }

    public static Economy getEconomyByName(String name){
        for (Economy economy : getEconomies()){
            if (economy.getName().equalsIgnoreCase(name)){
                return economy;
            }
        }
        return null;
    }
}