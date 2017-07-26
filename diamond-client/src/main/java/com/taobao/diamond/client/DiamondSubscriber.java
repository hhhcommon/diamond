package com.taobao.diamond.client;

import java.util.List;
import java.util.Set;



@Deprecated
public interface DiamondSubscriber extends DiamondClientSub {

   
    String getAvailableConfigureInfomation(String dataId, String group, long timeout);
    
    
    String getConfigureInfomation(String dataId, String group, long timeout);

   
    Set<String> getDataIds();
    
    List<String> getServerList();
    
}
