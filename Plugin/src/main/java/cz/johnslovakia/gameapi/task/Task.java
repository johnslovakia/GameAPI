package cz.johnslovakia.gameapi.task;


import cz.johnslovakia.gameapi.events.TaskEvent;
import cz.johnslovakia.gameapi.game.Game;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class Task {

    private Integer startCounter = 10;
    private int counter = 10;
    private double doubleCounter = 10.0D;
    private boolean doubleCounterBoolean = false;
    private int restartCount = 0;
    private Plugin plugin;

    private String id;
    private Game game;

    private BukkitTask doubleTask;
    private BukkitTask integerTask;

    private TaskInterface taskInterface;

    private static final List<Task> tasks = new ArrayList<>();

    private Task task;

    public Task(Game game, String id, int startTime, Plugin plugin) {
        task = this;

        this.game = game;
        this.id = id;
        this.plugin = plugin;

        this.startCounter = startTime;
        this.counter = startTime;
        this.doubleCounter = startCounter.doubleValue();

        start();
        tasks.add(this);
    }

    public Task(Game game, String id, int startTime, TaskInterface taskInterface, Plugin plugin) {
        task = this;

        this.game = game;
        this.id = id;
        this.plugin = plugin;
        this.taskInterface = taskInterface;

        this.startCounter = startTime;
        this.counter = startTime;
        this.doubleCounter = startCounter.doubleValue();

        start();
        tasks.add(this);
    }

    public Task(Game game, String id, int startTime, boolean doubleCounter, TaskInterface taskInterface, Plugin plugin) {
        task = this;

        this.game = game;
        this.id = id;
        this.plugin = plugin;
        this.taskInterface = taskInterface;

        this.doubleCounterBoolean = doubleCounter;
        this.doubleCounter = startCounter.doubleValue();
        this.startCounter = startTime;
        this.counter = startTime;

        start();
        tasks.add(this);
    }

    public Task getThisTask(){
        return this;
    }

    public void start(){
        if (taskInterface != null) {
            taskInterface.onStart(getThisTask());
        }

        if (doubleCounterBoolean) {
            doubleTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (getDoubleCounter() == 0) {
                        doubleCounter = startCounter.doubleValue();
                        this.cancel();
                        //cancelRunnable(true);
                    } else {
                        doubleCounter = doubleCounter - 0.1D;
                    }
                }
            }.runTaskTimer(this.plugin, 1, 1);
        }

        integerTask = new BukkitRunnable(){
            @Override
            public void run() {
                if (counter == 0) {
                    counter = startCounter;
                    cancelRunnable(true);
                    if (taskInterface != null) {
                        taskInterface.onEnd(getThisTask());
                    }
                } else {
                    counter--;
                    if (taskInterface != null) {
                        taskInterface.onCount(getThisTask());
                    }
                }

                TaskEvent ev = new TaskEvent(task);
                Bukkit.getPluginManager().callEvent(ev);
            }
        }.runTaskTimer(this.plugin, 0, 20L);
    }

    public void cancel(){
        if (doubleTask != null) {
            doubleTask.cancel();
        }
        if (integerTask != null) {
            integerTask.cancel();
        }
    }

    public Integer getRestartCount(){
        return restartCount;
    }

    public void setStartCounter(int startCounter) {
        this.startCounter = startCounter;
    }

    public void restart(){
        cancelRunnable(true);
        Task task = new Task(getGame(), getId(), getStartCounter(), getTaskInterface(), getPlugin());
        task.setRestartCount(restartCount + 1);
    }

    public void setGame(Game game){
        game.setRunningMainTask(this);
    }



    public static List<Task> getTasks() {
        return tasks;
    }

    public void cancelRunnable(boolean b) {
        //super.cancel();
        if (doubleTask != null) {
            doubleTask.cancel();
        }
        if (integerTask != null) {
            integerTask.cancel();
        }
        if (b) {
            tasks.remove(this);
        }
    }

    public static void cancel(Game game, String id) {
        if (getTask(game, id) == null){
            return;
        }
        getTask(game, id).cancelRunnable(true);
    }

    public static Task getTask(Game game, String id){
        for (Task task : tasks){
            if (task.getGame().equals(game)) {
                if (task.getId().equals(id)) {
                    return task;
                }
            }
        }
        return null;
    }

    public static void cancelAll(Game game) {
        getTasks().stream().filter(t -> t.getGame().equals(game)).toList().forEach(t -> t.cancelRunnable(true));
        tasks.removeIf(e -> e.getGame().equals(game));

    }
}