package com.taobao.diamond.notify.utils.task;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.ObjectName;

import com.taobao.middleware.logger.Logger;


import com.taobao.diamond.common.Constants;
import com.taobao.diamond.utils.LogUtils;


public final class TaskManager implements TaskManagerMBean {
    
    private static final Logger log = LogUtils.logger(TaskManager.class);

    private final ConcurrentHashMap<String, Task> tasks = new ConcurrentHashMap<String, Task>();

    private final ConcurrentHashMap<String, TaskProcessor> taskProcessors =
            new ConcurrentHashMap<String, TaskProcessor>();

    private TaskProcessor defaultTaskProcessor;

    Thread processingThread;

    private final AtomicBoolean closed = new AtomicBoolean(true);
    
    private String name;

    class ProcessRunnable implements Runnable {

        public void run() {
            while (!TaskManager.this.closed.get()) {
                try {
                    Thread.sleep(100);
                    TaskManager.this.process();
                }
                catch (Throwable e) {
                }
            }

        }

    }

    ReentrantLock lock = new ReentrantLock();

    Condition notEmpty = this.lock.newCondition();


    public TaskManager() {
        this(null);
    }


    public Task getTask(String type) {
        return this.tasks.get(type);
    }


    public TaskProcessor getTaskProcessor(String type) {
        return this.taskProcessors.get(type);
    }


    public TaskManager(String name) {
        this.name = name;
        if (null != name && name.length() > 0) {
            this.processingThread = new Thread(new ProcessRunnable(), name);
        }
        else {
            this.processingThread = new Thread(new ProcessRunnable());
        }
        this.processingThread.setDaemon(true);
        this.closed.set(false);
        this.processingThread.start();
    }

    public int size() {
        return tasks.size();
    }

    public void close() {
        this.closed.set(true);
        this.processingThread.interrupt();
    }


    public void await() throws InterruptedException {
        this.lock.lock();
        try {
            while (!this.isEmpty()) {
                this.notEmpty.await();
            }
        }
        finally {
            this.lock.unlock();
        }
    }


    public void await(long timeout, TimeUnit unit) throws InterruptedException {
        this.lock.lock();
        try {
            while (!this.isEmpty()) {
                this.notEmpty.await(timeout, unit);
            }
        }
        finally {
            this.lock.unlock();
        }
    }


    public void addProcessor(String type, TaskProcessor taskProcessor) {
        this.taskProcessors.put(type, taskProcessor);
    }


    public void removeProcessor(String type) {
        this.taskProcessors.remove(type);
    }


    public void removeTask(String type) {
        this.lock.lock();
        try {
            this.tasks.remove(type);
        }
        finally {
            this.lock.unlock();
        }
    }


    public void addTask(String type, Task task) {
        this.lock.lock();
        try {
            Task oldTask = tasks.put(type, task);
            if (null != oldTask) {
                task.merge(oldTask);
            }
        } finally {
            this.lock.unlock();
        }
    }


    protected void process() {
        for (Map.Entry<String, Task> entry : this.tasks.entrySet()) {
            Task task = null;
            this.lock.lock();
            try {
                task = entry.getValue();
                if (null != task) {
                    if (!task.shouldProcess()) {
                        continue;
                    }
                    this.tasks.remove(entry.getKey());
                }
            }
            finally {
                this.lock.unlock();
            }

            if (null != task) {
                TaskProcessor processor = this.taskProcessors.get(entry.getKey());
                if (null == processor) {
                    processor = this.getDefaultTaskProcessor();
                }
                if (null != processor) {
                    boolean result = false;
                    try {
                        result = processor.process(entry.getKey(), task);
                    }
                    catch (Throwable t) {
						log.error("task_fail", "", t);
                    }
                    if (!result) {
                        task.setLastProcessTime(System.currentTimeMillis());

                        this.addTask(entry.getKey(), task);
                    }
                }
            }
        }

        if (tasks.isEmpty()) {
            this.lock.lock();
            try {
                this.notEmpty.signalAll();
            }
            finally {
                this.lock.unlock();
            }
        }
    }


    public boolean isEmpty() {
        return tasks.isEmpty();
    }


    public TaskProcessor getDefaultTaskProcessor() {
        this.lock.lock();
        try {
            return this.defaultTaskProcessor;
        }
        finally {
            this.lock.unlock();
        }
    }


    public void setDefaultTaskProcessor(TaskProcessor defaultTaskProcessor) {
        this.lock.lock();
        try {
            this.defaultTaskProcessor = defaultTaskProcessor;
        }
        finally {
            this.lock.unlock();
        }
    }


    public String getTaskInfos() {
        StringBuilder sb = new StringBuilder();
        for(String taskType: this.taskProcessors.keySet()) {
            sb.append(taskType).append(":");
            Task task = this.tasks.get(taskType);
            if(task != null) {
                sb.append(new Date(task.getLastProcessTime()).toString());
            } else {
                sb.append("finished");
            }
            sb.append(Constants.DIAMOND_LINE_SEPARATOR);
        }
        
        return sb.toString();
    }
    
    
    public void init() {
        try {
            ObjectName oName = new ObjectName(this.name + ":type=" + TaskManager.class.getSimpleName());
            ManagementFactory.getPlatformMBeanServer().registerMBean(this, oName);
        }
        catch (Exception e) {
			log.error("registerMBean_fail", "", e);
        }
    }
}
