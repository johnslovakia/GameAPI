package cz.johnslovakia.gameapi.users.resources;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class Resource {

    public static List<Resource> resourceList = new ArrayList<>();


    @Getter
    private String name, img_char;
    @Getter
    private ChatColor chatColor;
    @Getter
    private int rank;

    @Getter @Setter
    private ResourceInterface resourceInterface;


    /**
     * To make the economy work, set the economy using setOnAdd(), setOnRemove() and setBallanceRunnable()
     * or by using setEconomyAutomatically() method this will be set itself and link it to the database
     * tables created by GameAPI.
     *
     * @param rank In case of multiple economies. Set the order in which Economy rewards
     *             are listed at the end of the game.
     */
    public Resource(String name, ChatColor chatColor, int rank, ResourceInterface resourceInterface) {
        this.name = name;
        this.chatColor = chatColor;
        this.rank = rank;
        this.resourceInterface = resourceInterface;

        resourceList.add(this);
    }

    public Resource(String name, String img_char, ChatColor chatColor, int rank, ResourceInterface resourceInterface) {
        this.name = name;
        this.img_char = img_char;
        this.chatColor = chatColor;
        this.rank = rank;
        this.resourceInterface = resourceInterface;

        resourceList.add(this);
    }

    public Resource(String name, ChatColor chatColor, int rank, boolean automatically, boolean forAllMinigames) {
        this.name = name;
        this.chatColor = chatColor;
        this.rank = rank;
        this.automatically = automatically;
        this.forAllMinigames = forAllMinigames;

        resourceList.add(this);
    }

    public Resource(String name, String img_char, ChatColor chatColor, int rank, boolean automatically, boolean forAllMinigames) {
        this.name = name;
        this.img_char = img_char;
        this.chatColor = chatColor;
        this.rank = rank;
        this.automatically = automatically;
        this.forAllMinigames = forAllMinigames;

        resourceList.add(this);
    }

    @Getter
    private boolean automatically, forAllMinigames;

    /**
     * @param forAllMinigames If is set to true, all minigames will have a linked resource.
     *                        If is set to false, the Minigame Table must be assigned before executing this method.
     */
    public void setAutomatically(boolean automatically, boolean forAllMinigames){
        this.automatically = automatically;
        this.forAllMinigames = forAllMinigames;
    }

    public String formattedName() {
        String caps = name.toLowerCase();
        return caps.substring(0, 1).toUpperCase() + caps.substring(1);
    }

    public static List<Resource> getResources() {
        return resourceList;
    }

    public static Resource getResourceByName(String name){
        for (Resource resource : getResources()){
            if (resource.getName().equalsIgnoreCase(name)){
                return resource;
            }
        }
        return null;
    }
}