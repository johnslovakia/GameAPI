package cz.johnslovakia.gameapi.task;

public interface TaskInterface {

    void onStart(Task task);
    void onCount(Task task);
    void onEnd(Task task);
}
