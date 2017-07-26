package com.taobao.diamond.client;

import com.taobao.diamond.client.impl.*;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.manager.ManagerListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static com.taobao.diamond.client.impl.DiamondEnvRepo.defaultEnv;


public class Diamond {


    static public void addListener(String dataId, String group, ManagerListener listener) {
        defaultEnv.addListeners(dataId, group, Arrays.asList(listener));
    }
    

    static public void addListeners(String dataId, String group, List<ManagerListener> listeners) {
        defaultEnv.addListeners(dataId, group, listeners);
    }


    static public void removeListener(String dataId, String group, ManagerListener listener) {
        defaultEnv.removeListener(dataId, group, listener);
    }
    

    static public List<ManagerListener> getListeners(String dataId, String group) {
        return defaultEnv.getListeners(dataId, group);
    }

    static public String getConfig(String dataId, String group, long timeoutMs) throws IOException {
        return defaultEnv.getConfig(dataId, group, timeoutMs);
    }
    

    static public String getConfig(String dataId, String group, int feature, long timeoutMs) throws IOException{
    	return defaultEnv.getConfig(dataId, group, feature, timeoutMs);
    }
    

    public static String getConfigFromSnapShot(String dataId, String group){
    	return defaultEnv.getConfigFromSnapshot(dataId, group);
    }

    static public boolean publishSingle(String dataId, String group, String content) {
        return defaultEnv.publishSingle(dataId, group, content);
    }

    static public boolean publishSingle(String dataId, String group, String appName, String content) {
    	return defaultEnv.publishSingle(dataId, group, appName, content);
    }


    static public boolean publishAggr(String dataId, String group, String datumId, String content) {
        return defaultEnv.publishAggr(dataId, group, datumId, content);
    }
    

    static public boolean publishAggr(String dataId, String group, String datumId, String appName, String content) {
    	return defaultEnv.publishAggr(dataId, group, datumId, appName, content);
    }

    static public boolean remove(String dataId, String group) {
        return defaultEnv.remove(dataId, group);
    }

    static public boolean removeAggr(String dataId, String group, String datumId) {
        return defaultEnv.removeAggr(dataId, group, datumId);
    }


    static public DiamondEnv getTargetEnv(String... serverIps) {
        return DiamondEnvRepo.getTargetEnv(serverIps);
    }


    static public DiamondEnv getTargetEnv (String host, int port) {
        return DiamondEnvRepo.getTargetEnv(host, port);
    }
    

    static public List<DiamondEnv> allDiamondEnvs() {
        return DiamondEnvRepo.allDiamondEnvs();
    }

 
    static public BatchHttpResult<ConfigInfoEx> batchQuery(List<String> dataIds, String group,
            long timeoutMs) {
        return defaultEnv.batchQuery(dataIds, group, timeoutMs);
    }


    static public BatchHttpResult<ConfigInfoEx> batchGetConfig(List<String> dataIds, String group,
                                                           long timeoutMs) {
        return defaultEnv.batchGetConfig(dataIds, group, timeoutMs);
    }


	public static boolean batchRemoveAggr(String dataId, String group, List<String> datumIdList, long timeoutMs) throws IOException {
		return defaultEnv.batchRemoveAggr(dataId, group, datumIdList, timeoutMs);
	}
	

	public static boolean batchPublishAggr(String dataId, String group, Map<String, String> datumMap, long timeoutMs) throws IOException{
		return defaultEnv.batchPublishAggr(dataId, group, datumMap, timeoutMs);
	}
	

	public static boolean batchPublishAggr(String dataId, String group, Map<String, String> datumMap, String appName, long timeoutMs) throws IOException{
		return defaultEnv.batchPublishAggr(dataId, group, datumMap, appName, timeoutMs);
	}
	

	public static boolean replaceAggr(String dataId, String group, Map<String, String> datumMap, long timeoutMs) throws IOException{
		return defaultEnv.replaceAggr(dataId, group, datumMap, timeoutMs);
	}
	

	public static boolean replaceAggr(String dataId, String group, Map<String, String> datumMap, String appName, long timeoutMs) throws IOException{
		return defaultEnv.replaceAggr(dataId, group, datumMap, appName, timeoutMs);
	}
    
}
