package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.task.Task;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class TaskEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Task task;
    private final Type type;

    public TaskEvent(Task task, Type type) {
        this.task = task;
        this.type = type;
    }


    public String getId() {
        return task.getId();
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

    public enum Type { START, TICK, END }
}