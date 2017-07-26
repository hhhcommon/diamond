package com.taobao.diamond.notify.utils.task;

/**
 * @author huali
 *
 */
public abstract class Task {
	private long taskInterval;
	
	private long lastProcessTime;
	
	public abstract void merge(Task task);
	
	public void setTaskInterval(long interval){
		this.taskInterval = interval;
	}
	
	public long getTaskInterval(){
		return this.taskInterval;
	}
	
	public void setLastProcessTime(long lastProcessTime){
		this.lastProcessTime = lastProcessTime;
	}
	
	public long getLastProcessTime(){
		return this.lastProcessTime;
	}
	
	public boolean shouldProcess(){
		return (System.currentTimeMillis() - this.lastProcessTime >= this.taskInterval);
	}
	
}
