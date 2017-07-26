package com.taobao.diamond.manager;

import java.util.concurrent.Executor;


public interface ManagerListener {

    public Executor getExecutor();


    public void receiveConfigInfo(final String configInfo);
}
