package cz.johnslovakia.gameapi.modules.game.task;


import cz.johnslovakia.gameapi.events.TaskEvent;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
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

    private long startTime;
    private Integer startCounter = 10;
    private int counter;
    private double doubleCounter;
    private boolean doubleCounterBoolean = false;
    private int restartCount = 0;
    private final Plugin plugin;

    private String id;
    private GameInstance game;
    private TaskModule taskModule;

    private BukkitTask doubleTask;
    private BukkitTask integerTask;

    private TaskInterface taskInterface;

    private final Task task;

    public Task(GameInstance game, String id, int startTime, Plugin plugin) {
        task = this;

        this.id = id;
        this.game = game;
        this.taskModule = game.getModule(TaskModule.class);
        this.plugin = plugin;

        this.startCounter = startTime;
        this.counter = startTime;
        this.doubleCounter = startCounter.doubleValue();

        start();
    }

    public Task(GameInstance game, String id, int startTime, TaskInterface taskInterface, Plugin plugin) {
        task = this;

        this.id = id;
        this.game = game;
        this.taskModule = game.getModule(TaskModule.class);
        this.plugin = plugin;
        this.taskInterface = taskInterface;

        this.startCounter = startTime;
        this.counter = startTime;
        this.doubleCounter = startCounter.doubleValue();

        start();
    }

    public Task(GameInstance game, String id, int startTime, boolean doubleCounter, TaskInterface taskInterface, Plugin plugin) {
        task = this;

        this.id = id;
        this.game = game;
        this.taskModule = game.getModule(TaskModule.class);
        this.plugin = plugin;
        this.taskInterface = taskInterface;

        this.doubleCounterBoolean = doubleCounter;
        this.doubleCounter = startCounter.doubleValue();
        this.startCounter = startTime;
        this.counter = startTime;

        start();
    }

    public Task getThisTask(){
        return this;
    }

    public void start() {
        if (taskInterface != null) {
            taskInterface.onStart(getThisTask());
        }

        startTime = System.currentTimeMillis();
        Bukkit.getPluginManager().callEvent(new TaskEvent(task, TaskEvent.Type.START));

        if (doubleCounterBoolean) {
            doubleTask = new BukkitRunnable() {
                @Override
                public void run() {
                    doubleCounter -= 0.1D;

                    if (doubleCounter <= 0) {
                        doubleCounter = startCounter.doubleValue();
                        this.cancel();
                    }
                }
            }.runTaskTimer(this.plugin, 1, 1);
        }

        integerTask = new BukkitRunnable() {

            private boolean firstTick = true;

            @Override
            public void run() {
                if (firstTick) {
                    firstTick = false;
                    return;
                }

                counter--;

                if (taskInterface != null) {
                    if (counter == 0) {
                        taskModule.cancel(task, true);
                        taskInterface.onEnd(getThisTask());
                    } else {
                        taskInterface.onCount(getThisTask());
                    }
                }

                Bukkit.getPluginManager().callEvent(new TaskEvent(task, TaskEvent.Type.TICK));

                if (counter == 0) {
                    Bukkit.getPluginManager().callEvent(new TaskEvent(task, TaskEvent.Type.END));

                    counter = startCounter;
                    this.cancel();
                }
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
        if (getTaskInterface() != null) {
            getTaskInterface().onCancel(this);
        }
    }

    public Integer getRestartCount(){
        return restartCount;
    }

    public void setStartCounter(int startCounter) {
        this.startCounter = startCounter;
    }

    public void restart(){
        taskModule.cancel(this, true);
        Task task = taskModule.addTask(new Task(getGame(), getId(), getStartCounter(), getTaskInterface(), getPlugin()));
        task.setRestartCount(restartCount + 1);
    }

    public void setAsMainTask(){
        game.setRunningMainTask(this);
    }
}
