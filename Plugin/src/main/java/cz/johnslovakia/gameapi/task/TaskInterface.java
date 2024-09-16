package cz.johnslovakia.gameapi.task;

public interface TaskInterface {

    public void onStart(Task task);
    public void onCount(Task task);
    public void onEnd(Task task);
}
