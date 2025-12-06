package cz.johnslovakia.gameapi.modules.game.task;

public interface TaskInterface {

    default void onStart(Task task){}
    default void onCount(Task task){}
    default void onEnd(Task task){}
    default void onCancel(Task task){}
}
