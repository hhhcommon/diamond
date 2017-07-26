package com.taobao.diamond.manager;

import com.taobao.diamond.utils.StringUtils;



public abstract class SkipInitialCallbackListener implements ManagerListener {

    private final String initialValue;
    private boolean hasCallbacked = false;


    public SkipInitialCallbackListener(String initialConfig) {
        initialValue = initialConfig;
    }


    public void receiveConfigInfo(final String configInfo) {
        if (!hasCallbacked) {
            hasCallbacked = true;

            if (StringUtils.equals(initialValue, configInfo)) {
                return;
            }
        }

        receiveConfigInfo0(configInfo);
    }
    
    abstract public void receiveConfigInfo0(String configInfo);
}
