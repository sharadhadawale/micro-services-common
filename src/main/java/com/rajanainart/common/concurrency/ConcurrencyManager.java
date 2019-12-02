package com.rajanainart.common.concurrency;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ConcurrencyManager implements Closeable {
    private static ExecutorService executorService = Executors.newFixedThreadPool(50);

    private String operation;
    private List<BaseConcurrencyThread> threads;

    public String getOperationName() { return operation; }

    public ConcurrencyManager(String operation) {
        this.operation = operation;
        threads        = new ArrayList<>();
    }

    public void submit(BaseConcurrencyThread thread) {
        executorService.submit(thread);
        threads.add(thread);
    }

    public void awaitTermination(LogCallback callback, int completedCallback) {
        long startTime = System.currentTimeMillis();
        Map<String, Integer> completed = new HashMap<>();
        int lastCompleted = 0;
        while (true) {
            for (BaseConcurrencyThread t : threads) {
                if (t.getIsComplete()) {
                    completed.put(t.getThreadName(), 1);
                    if (completed.size() % completedCallback == 0 && completed.size() > lastCompleted) {
                        String message = String.format("%s threads out of %s total threads [pool] completed, time elapsed in seconds %s",
                                                        completed.size(), threads.size(), (System.currentTimeMillis() - startTime)/1000);
                        callback.log(message);
                        lastCompleted = completed.size();
                    }
                }
            }
            if (completed.size() >= threads.size()) break;
        }
        callback.log(String.format("Total completed threads: %s", completed.size()));
    }

    @Override
    public void close() {
        if (threads != null) threads.clear();
    }

    public interface BaseConcurrencyThread extends Runnable {
        boolean getIsComplete();
        String  getThreadName();
    }

    @FunctionalInterface
    public interface LogCallback {
        void log(String message);
    }
}
