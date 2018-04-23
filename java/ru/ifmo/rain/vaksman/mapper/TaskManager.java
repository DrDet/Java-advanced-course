package ru.ifmo.rain.vaksman.mapper;

public class TaskManager {
    private int remain;

    public TaskManager(int cntTasks) {
        remain = cntTasks;
    }

    private boolean isDone() {
        return remain == 0;
    }

    public synchronized void waitForResult() throws InterruptedException {
        while (!isDone()) {
            wait();
        }
    }

    public synchronized void done() {
        remain--;
        if (isDone()) {
            notify();
        }
    }
}
