package com.taobao.diamond.client;

@Deprecated
public interface DiamondPublisher {

    void setClusterType(ClusterType clusterType);

    void publish(String dataId, String group, String configInfo, ContentIdentityPattern pattern);

    boolean syncPublish(String dataId, String group, String configInfo, long timeout,
            ContentIdentityPattern pattern);

    void publishAll(String dataId, String group, String configInfo);
    
    boolean syncPublishAll(String dataId, String group, String configInfo, long timeout);
    
    void unpublishAll(String dataId, String group);
    
    boolean syncUnpublishAll(String dataId, String group, long timeout);
    
    DiamondConfigure getDiamondConfigure();
    
    void setDiamondConfigure(DiamondConfigure diamondConfigure);
    
    void start();
    
    void close();
    
    boolean addDatum(String dataId, String group, String datumId, String configInfo, long timeout);

    boolean deleteDatum(String dataId, String group, String datumId, long timeout);

    boolean deleteAllDatums(String dataId, String group, long timeout);
    


}
