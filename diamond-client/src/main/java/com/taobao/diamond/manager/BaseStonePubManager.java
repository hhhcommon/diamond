package com.taobao.diamond.manager;

import com.taobao.diamond.client.ContentIdentityPattern;
import com.taobao.diamond.client.DiamondConfigure;


@Deprecated
public interface BaseStonePubManager {

    void publish(String dataId, String group, String configInfo, ContentIdentityPattern pattern);

    boolean syncPublish(String dataId, String group, String configInfo, long timeout,
            ContentIdentityPattern pattern);
    
    void publishAll(String dataId, String group, String configInfo);
    
    boolean syncPublishAll(String dataId, String group, String configInfo, long timeout);


    void removeAll(String dataId, String group);

    boolean syncRemoveAll(String dataId, String group, long timeout);



    DiamondConfigure getDiamondConfigure();


    void setDiamondConfigure(DiamondConfigure diamondConfigure);


    void close();


    boolean publish(String dataId, String group, String datumId, String configInfo, long timeout);

    boolean unPublish(String dataId, String group, String datumId, long timeout);

    boolean unPublishAll(String dataId, String group, long timeout);
}
