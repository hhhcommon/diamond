package com.taobao.diamond.manager.impl;

import java.util.Arrays;
import java.util.List;

import com.taobao.diamond.manager.BaseStoneSubManager;
import com.taobao.diamond.manager.ManagerListener;

@Deprecated
public class DefaultBaseStoneSubManager extends DefaultDiamondManager implements
        BaseStoneSubManager {


    public DefaultBaseStoneSubManager(String dataId, String group, ManagerListener managerListener) {
        super(group, dataId, Arrays.asList(managerListener));
    }


    public DefaultBaseStoneSubManager(String dataId, String group,
            List<ManagerListener> managerListenerList) {
        super(group, dataId, managerListenerList);
    }

}
