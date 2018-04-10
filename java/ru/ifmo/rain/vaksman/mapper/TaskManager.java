package ru.ifmo.rain.vaksman.mapper;

class TaskManager {
    private int remain;

    TaskManager(int cntTasks) {
        remain = cntTasks;
    }

    synchronized void count() {
        remain--;
    }

    boolean isDone() {
        return remain == 0;
    }

    synchronized void waitForResult() throws InterruptedException {
        while (!isDone()) {
            wait();
        }
    }
}
