package ru.ifmo.rain.vaksman.mapper;

import java.util.List;
import java.util.function.Function;

public class Task<T, R> {
    private final Function<? super T, ? extends R> f;
    private final T arg;
    private final List<R> dstL;
    private final int dstI;
    private final TaskManager manager;

    public Task(Function<? super T, ? extends R> f, T arg, List<R> dstL, int dstI, TaskManager manager) {
        this.f = f;
        this.arg = arg;
        this.dstL = dstL;
        this.dstI = dstI;
        this.manager = manager;
    }

    public void execute() {
        dstL.set(dstI, f.apply(arg));
        manager.count();
        synchronized (manager) {
            if (manager.isDone()) {
                manager.notify();
            }
        }
    }
}
