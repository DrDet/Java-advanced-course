package ru.ifmo.rain.vaksman.mapper;

import java.util.LinkedList;
import java.util.Queue;

public class TaskQueue {
    private static final int MAX_SIZE = 1_000_000;
    private final Queue<Task> tasks;

    public TaskQueue() {
        this.tasks = new LinkedList<>();
    }

    public synchronized Task getTask() throws InterruptedException {
        while (tasks.isEmpty()) {
            wait();
        }
        Task task = tasks.poll();
        notifyAll();
        return task;
    }

    public synchronized void putTask(Task task) throws InterruptedException {
        while (tasks.size() == MAX_SIZE) {
            wait();
        }
        tasks.add(task);
        notifyAll();
    }
}
