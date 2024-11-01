package cz.johnslovakia.gameapi.task;

public interface TaskInterface {

    default void onStart(Task task){}
    default void onCount(Task task){}
    default void onEnd(Task task){}
}
