package cz.johnslovakia.gameapi.users.resources;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.datastorage.MinigameTable;
import cz.johnslovakia.gameapi.datastorage.PlayerTable;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.Row;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class Resource {


    private String name;
    @Setter
    private String displayName;
    private ChatColor color;
    private int rank;

    @Setter
    private int firstDailyWinReward;
    @Setter
    private boolean applicableBonus = false;
    @Setter
    private String img_char;
    @Setter
    private ResourceInterface resourceInterface;

    /**
     * @param rank In case of multiple economies. Set the order in which Economy rewards
     *             are listed at the end of the game.
     */
    public Resource(String name, ChatColor color, int rank, ResourceInterface resourceInterface) {
        this.name = name;
        this.color = color;
        this.rank = rank;
        this.resourceInterface = resourceInterface;

        //TODO: dát to vždy přímo u vytváření toho resource v pluginech
        if (name.equalsIgnoreCase("Coins")) applicableBonus = true;
    }

    public Resource(String name, ChatColor color, int rank, boolean automatically, boolean forAllMinigames) {
        this.name = name;
        this.color = color;
        this.rank = rank;
        setAutomatically(automatically, forAllMinigames);

        //TODO: dát to vždy přímo u vytváření toho resource v pluginech
        if (name.equalsIgnoreCase("Coins")) applicableBonus = true;
    }

    public Resource(String name, ChatColor color, ResourceInterface resourceInterface) {
        this.name = name;
        this.color = color;
        this.resourceInterface = resourceInterface;

        //TODO: dát to vždy přímo u vytváření toho resource v pluginech
        if (name.equalsIgnoreCase("Coins")) applicableBonus = true;
    }

    public Resource(String name, ChatColor color, boolean automatically, boolean forAllMinigames) {
        this.name = name;
        this.color = color;
        setAutomatically(automatically, forAllMinigames);

        //TODO: dát to vždy přímo u vytváření toho resource v pluginech
        if (name.equalsIgnoreCase("Coins")) applicableBonus = true;
    }

    public String getDisplayName(){
        if (displayName != null)
            return displayName;
        return name;
    }

    @Getter
    private boolean automatically, forAllMinigames;

    /**
     * @param forAllMinigames If is set to true, all minigames will have a linked 
     *                        If is set to false, the Minigame Table must be assigned before executing this method.
     */
    public void setAutomatically(boolean automatically, boolean forAllMinigames){
        this.automatically = automatically;
        this.forAllMinigames = forAllMinigames;
        
        if (!automatically)
            return;

        if (!forAllMinigames){
            this.resourceInterface = new ResourceInterface() {
                @Override
                public void deposit(GamePlayer gamePlayer, int amount) {
                    MinigameTable minigameTable = Minigame.getInstance().getMinigameTable();
                    SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
                    Optional<Row> result = connection.select()
                            .from(minigameTable.getTableName())
                            .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                            .obtainOne();

                    result.ifPresent(row -> connection.update()
                            .table(minigameTable.getTableName())
                            .set(getName(), row.getInt(getName()) + amount)
                            .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                            .execute());
                }

                @Override
                public void withdraw(GamePlayer gamePlayer, int amount) {
                    MinigameTable minigameTable = Minigame.getInstance().getMinigameTable();
                    SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
                    Optional<Row> result = connection.select()
                            .from(minigameTable.getTableName())
                            .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                            .obtainOne();

                    result.ifPresent(row -> connection.update()
                            .table(minigameTable.getTableName())
                            .set(getName(), row.getInt(getName()) - amount)
                            .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                            .execute());
                }

                @Override
                public void setBalance(GamePlayer gamePlayer, int balance) {
                    MinigameTable minigameTable = Minigame.getInstance().getMinigameTable();
                    SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
                    connection.update()
                            .table(minigameTable.getTableName())
                            .set(getName(), balance)
                            .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                            .execute();
                }

                @Override
                public int getBalance(GamePlayer gamePlayer) {
                    MinigameTable minigameTable = Minigame.getInstance().getMinigameTable();
                    SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
                    Optional<Row> result = connection.select()
                            .from(minigameTable.getTableName())
                            .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                            .obtainOne();

                    if (result.isPresent()){
                        if (result.get().get(getName()) != null) {
                            return result.get().getInt(getName());
                        }
                    }
                    return 0;
                }
            };
        }else{
            this.resourceInterface = new ResourceInterface() {
                @Override
                public void deposit(GamePlayer gamePlayer, int amount) {
                    SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
                    Optional<Row> result = connection.select()
                            .from(PlayerTable.TABLE_NAME)
                            .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                            .obtainOne();

                    connection.update()
                            .table(PlayerTable.TABLE_NAME)
                            .set(getName(), result.get().getInt(getName()) + amount)
                            .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                            .execute();
                }

                @Override
                public void withdraw(GamePlayer gamePlayer, int amount) {
                    SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
                    Optional<Row> result = connection.select()
                            .from(PlayerTable.TABLE_NAME)
                            .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                            .obtainOne();

                    connection.update()
                            .table(PlayerTable.TABLE_NAME)
                            .set(getName(), result.get().getInt(getName()) - amount)
                            .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                            .execute();
                }

                @Override
                public void setBalance(GamePlayer gamePlayer, int balance) {
                    SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
                    connection.update()
                            .table(PlayerTable.TABLE_NAME)
                            .set(getName(), balance)
                            .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                            .execute();
                }

                @Override
                public int getBalance(GamePlayer gamePlayer) {
                    SQLDatabaseConnection connection = Minigame.getInstance().getDatabase().getConnection();
                    Optional<Row> result = connection.select()
                            .from(PlayerTable.TABLE_NAME)
                            .where().isEqual("Nickname", gamePlayer.getOnlinePlayer().getName())
                            .obtainOne();

                    if (result.isPresent()){
                        if (result.get().get(getName()) != null) {
                            return result.get().getInt(getName());
                        }
                    }
                    return 0;
                }
            };
        }
    }

    public String formattedName() {
        String caps = name.toLowerCase();
        return caps.substring(0, 1).toUpperCase() + caps.substring(1);
    }

    public ObservableResource observe(@NotNull ResourceChangeListener listener) {
        ResourceInterface currentInterface = this.resourceInterface;
        if (currentInterface == null) return null;
        ObservableResource observable;

        if (!(currentInterface instanceof ObservableResource)) {
            observable = new ObservableResource(currentInterface);
            this.resourceInterface = observable;
        } else {
            observable = (ObservableResource) currentInterface;
        }

        observable.addListener(listener);
        return observable;
    }
}