package com.taobao.diamond.manager;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.taobao.diamond.client.DiamondConfigure;



@Deprecated
public interface DiamondManager {

    void addListeners(List<ManagerListener> newListeners);
    

    void clearSelfListener();
    
    

    @Deprecated
    public void setManagerListener(ManagerListener managerListener);



    @Deprecated
    public void setManagerListeners(List<ManagerListener> managerListenerList);


    public List<ManagerListener> getManagerListeners();


    public String getConfigureInfomation(long timeoutMs);

    public String     getAvailableConfigureInfomation(long timeoutMs);
    public Properties getAvailablePropertiesConfigureInfomation(long timeoutMs);
    
    
    public void setDiamondConfigure(DiamondConfigure diamondConfigure);
    public DiamondConfigure getDiamondConfigure();


    public void close();


    public List<String> getServerAddress();


    public Set<String> getAllDataId();

}
