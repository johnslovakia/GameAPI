package cz.johnslovakia.gameapi.modules.game.task;

import cz.johnslovakia.gameapi.modules.game.GameModule;
import cz.johnslovakia.gameapi.utils.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TaskModule extends GameModule {

    private Map<String, Task> tasks = new HashMap<>();

    @Override
    protected void initialize() {

    }

    @Override
    protected void terminate() {
        cancelAll();
        tasks = null;
    }

    public Task addTask(Task task){
        if (tasks.containsKey(task.getId())){
            tasks.get(task.getId()).cancel();
        }
        tasks.put(task.getId(), task);
        task.setTaskModule(this);
        return task;
    }


    public void cancel(Task task, boolean remove) {
        task.cancel();
        if (remove) tasks.remove(task.getId());
    }

    public void cancel(String taskID, boolean remove) {
        Optional<Task> task = getTask(taskID);
        task.ifPresent(value -> cancel(value, remove));
    }

    public Optional<Task> getTask(String id){
        return Optional.ofNullable(tasks.get(id));
    }

    public void cancelAll() {
        tasks.values().forEach(Task::cancel);
        tasks.clear();
    }
}
