package com.taobao.diamond.utils;

import java.util.concurrent.atomic.AtomicLong;


/**
 * 
 * @author leiwen.zh
 * 
 */
public class TimeoutUtils {

    private final AtomicLong totalTime = new AtomicLong(0L);

    private volatile long lastResetTime;

    private volatile boolean initialized = false;

    private long totalTimeout;
    private long invalidThreshold;


    public TimeoutUtils(long totalTimeout, long invalidThreshold) {
        this.totalTimeout = totalTimeout;
        this.invalidThreshold = invalidThreshold;
    }


    public synchronized void initLastResetTime() {
        if (initialized) {
            return;
        }
        lastResetTime = System.currentTimeMillis();
        initialized = true;
    }


    public void addTotalTime(long time) {
        totalTime.addAndGet(time);
    }


    public boolean isTimeout() {
        return totalTime.get() > this.totalTimeout;
    }


    public void resetTotalTime() {
        if (isTotalTimeExpired()) {
            totalTime.set(0L);
            lastResetTime = System.currentTimeMillis();
        }
    }


    public AtomicLong getTotalTime() {
        return totalTime;
    }


    private boolean isTotalTimeExpired() {
        return System.currentTimeMillis() - lastResetTime > this.invalidThreshold;
    }
}
