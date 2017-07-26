package com.taobao.diamond.client.impl;

import com.taobao.diamond.client.impl.HttpSimpleClient.HttpResult;
import com.taobao.middleware.logger.support.LoggerHelper;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.taobao.diamond.client.impl.DiamondEnv.log;
import static com.taobao.diamond.client.impl.DiamondEnvRepo.defaultEnv;


public class DiamondUnitSite {


    static public boolean isInCenterUnit() {
        return serverMgr_default.getUrlString().equals(serverMgr_center.getUrlString());
    }
    

    static public void switchToCenterUnit() {
    	defaultEnv.initServerManager(serverMgr_center);
    }
    

    static public void switchToLocalUnit() {
        defaultEnv.initServerManager(serverMgr_default);
    }
    

    static public DiamondEnv getCenterUnitEnv() {
    	DiamondEnv env = DiamondEnvRepo.getUnitEnv("center");
        return env;
    }
    

    static public DiamondEnv getDiamondUnitEnv(String unitName) {
    	DiamondEnv env = DiamondEnvRepo.getUnitEnv(unitName);
        return env;
    }

    

    static public void publishToAllUnit(String dataId, String group, String content)
            throws IOException {
        for (DiamondEnv env : getUnitList()) {
            if (!env.publishSingle(dataId, group, content)) {
                log.error(env.getName(), "", "[pub-all] pub fail to unit, dataId={}, group={}", dataId, group);
                throw new IOException("pub fail to unit " + env + ", " + dataId + ", " + group);
            }
        }
    }
    

    static public void publishToAllUnit(String dataId, String group, String appName, String content)
    		throws IOException {
    	for (DiamondEnv env : getUnitList()) {
    		if (!env.publishSingle(dataId, group, appName, content)) {
    			log.error(env.getName(), "", "[pub-all] pub fail to unit, dataId={}, group={}", dataId, group);
    			throw new IOException("pub fail to unit " + env + ", " + dataId + ", " + group);
    		}
    	}
    }

    static public void removeToAllUnit(String dataId, String group) throws IOException {
        for (DiamondEnv env : getUnitList()) {
            if (!env.remove(dataId, group)) {
                log.error(env.getName(), "", "[remove-all] rm fail to unit, dataId={}, group={}", dataId, group);
                throw new IOException("rm fail to unit " + env + ", " + dataId + ", " + group);
            }
        }
    }
    

    static public List<DiamondEnv> getUnitList() throws IOException {
        List<String> unitNameList = null;

        HttpResult httpResult = HttpSimpleClient.httpGet(
                "http://"+ServerHttpAgent.domainName+":"+ServerHttpAgent.addressPort+"/diamond-server/unit-list?nofix=1", null, null, "GBK",
                1000L);

        if (HttpURLConnection.HTTP_OK == httpResult.code) {
            unitNameList = IOUtils.readLines(new StringReader(httpResult.content));
        } else {
            throw new IOException("http code " + httpResult.code + ", msg: " + httpResult.content);
        }

//        if (!unitNameList.contains("center")) {
//            unitNameList.add("center");
//        }
        List<DiamondEnv> envList = new ArrayList<DiamondEnv>(unitNameList.size());
        for (String unitName : unitNameList) {
            DiamondEnv env = DiamondEnvRepo.getUnitEnv(unitName);
            envList.add(env);
        }
        return envList;
    }

    // ==================

    static private ServerListManager serverMgr_default = new ServerListManager();
    static private ServerListManager serverMgr_center = new ServerManager_unitSite("center");

    static Map<String, ServerListManager> unit2serverMgr = new HashMap<String, ServerListManager>();
    
    static public  Map<String, Map<String, CacheData>> mockServerCache = null;
    
    static{
    	serverMgr_default.initServerList();
    	serverMgr_center.initServerList();
    }

}

class ServerManager_unitSite extends ServerListManager {

    public ServerManager_unitSite(String unitName) {
        unit = unitName;
        super.name = unitName;
        getServersUrl = "http://"+ServerHttpAgent.domainName+":"+ServerHttpAgent.addressPort+"/diamond-server/diamond-unit-" + unitName
                + "?nofix=1";
    }

    public synchronized void start() {
        if (isStarted || isFixed) {
            return;
        }

        GetServerListTask getServersTask = new GetServerListTask(getServersUrl);
        for (int i = 0; i < 3 && serverUrls.isEmpty(); ++i) {
            getServersTask.run();
            try {
                Thread.sleep(100L);
            } catch (Exception e) {
            }
        }

        if (serverUrls.isEmpty()) {
        	log.error("Diamond-0008", LoggerHelper.getErrorCodeStr("Diamond", "Diamond-0008", "环境问题","fail to get diamond-server serverlist"));
            log.error(name, "DIAMOND-XXXX", "[init-serverlist] fail to get diamond-server serverlist!");
            throw new RuntimeException("fail to get diamond-server serverlist! env:" + name);
        }
        
        TimerService.scheduleWithFixedDelay(getServersTask, 0L, 30L, TimeUnit.SECONDS);
        isStarted = true;
    }
    
    public void initServerList(){    	   	
        GetServerListTask getServersTask = new GetServerListTask(getServersUrl);
        for (int i = 0; i < 3 && serverUrls.isEmpty(); ++i) {
            getServersTask.run();
            try {
                Thread.sleep(100L);
            } catch (Exception e) {
            }
        }
    }
    
    
    @Override
    public String toString() {
        return "ServerManager-unit-" + unit+ "-" + getUrlString();
    }

    // ==========================
    final String unit;
    final String getServersUrl;
}
