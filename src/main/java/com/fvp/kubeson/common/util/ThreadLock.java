package com.fvp.kubeson.common.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class ThreadLock {

    private AtomicBoolean locked;

    private Thread lockedThread;

    public ThreadLock() {
        locked = new AtomicBoolean(false);
    }

    public void waitPermission() throws InterruptedException {
        if (locked.get()) {
            lockedThread = Thread.currentThread();
            LockSupport.park();
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
        }
    }

    public boolean isLocked() {
        return locked.get();
    }

    public void lock() {
        locked.set(true);
    }

    public void unlock() {
        locked.set(false);
        LockSupport.unpark(lockedThread);
    }
}
