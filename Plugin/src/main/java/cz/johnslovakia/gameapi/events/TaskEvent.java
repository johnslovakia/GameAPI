package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.task.Task;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TaskEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private Task task;

    public TaskEvent(Task task) {
        this.task = task;

    }

    public String getId() {
        return task.getId();
    }

    public Task getTask() {
        return task;
    }

    public int getCounter() {
        return task.getCounter();
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}