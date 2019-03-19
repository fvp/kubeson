package com.fvp.kubeson.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ThreadFactory {

    private static Logger LOGGER = LogManager.getLogger();

    private static List<Thread> threads = new ArrayList<>();

    private Thread thread;

    private ThreadFactory(Runnable runnable) {
        thread = new Thread(runnable);
        thread.setDaemon(true);
        threads.add(thread);
        thread.start();
    }

    public static ThreadFactory newThread(Runnable runnable) {
        return new ThreadFactory(runnable);
    }

    public static void shutdownAll() {
        LOGGER.info("Shutting down all {} Threads", threads.size());
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }

    public Thread getThread() {
        return thread;
    }

    public void interrupt() {
        thread.interrupt();
        threads.remove(thread);
    }

    public boolean isInterrupted() {
        return thread.isInterrupted();
    }
}
