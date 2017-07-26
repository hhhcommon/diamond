package com.taobao.diamond.client.impl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.codehaus.jackson.type.TypeReference;

import com.taobao.diamond.client.BatchHttpResult;
import com.taobao.diamond.client.Constant;
import com.taobao.diamond.client.impl.HttpSimpleClient.HttpResult;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.common.GroupKey;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.manager.ManagerListener;
import com.taobao.diamond.mockserver.MockServer;
import com.taobao.diamond.utils.ContentUtils;
import com.taobao.diamond.utils.JSONUtils;
import com.taobao.diamond.utils.StringUtils;
import com.taobao.middleware.logger.Logger;
import com.taobao.middleware.logger.support.LoggerHelper;
import com.taobao.spas.sdk.client.SpasSdkClientFacade;
import com.taobao.spas.sdk.common.sign.SpasSigner;



public class DiamondEnv {

    public void addListeners(String dataId, String group, List<? extends ManagerListener> listeners) {
        group = null2defaultGroup(group);

        CacheData cache = addCacheDataIfAbsent(dataId, group);
        for (ManagerListener listener : listeners) {
            cache.addListener(listener);
        }
    }
    
    public void removeListener(String dataId, String group, ManagerListener listener) {
        group = null2defaultGroup(group);

        CacheData cache = getCache(dataId, group);
        if (null != cache) {
            cache.removeListener(listener);
            if (cache.getListeners().isEmpty()) {
                removeCache(dataId, group);
            }
        }
    }
    
    public List<ManagerListener> getListeners(String dataId, String group) {
        group = null2defaultGroup(group);
        
        CacheData cache = getCache(dataId, group);
        if (null == cache) {
            return Collections.emptyList();
        }

        return cache.getListeners();
    }
    
   
    public String getConfig(String dataId, String group, long timeoutMs) throws IOException {
        group = null2defaultGroup(group);

        if (MockServer.isTestMode()) {
            return MockServer.getConfigInfo(dataId, group, this);
        }

        String content = LocalConfigInfoProcessor.getFailover(this, dataId, group);
        if (content != null) {
            log.warn(getName(), "[get-config] get failover ok, dataId={}, group={}, config={}", dataId, group, ContentUtils.truncateContent(content));
            return content;
        }

        try {
            return ClientWorker.getServerConfig(this, dataId, group, timeoutMs);
        } catch (IOException ioe) {
			if (Constant.NO_RIGHT.equals(ioe.getMessage())) {
				throw ioe;
			}
        	log.warn("Diamond-0003", LoggerHelper.getErrorCodeStr("Diamond", "Diamond-0003", "环境问题","get from server error"));
            log.warn(getName(), "[get-config] get from server error, dataId={}, group={}, msg={}", dataId, group, ioe.toString());
        }

        log.warn(getName(), "[get-config] get snapshot ok, dataId={}, group={}, config={}", dataId, group, ContentUtils.truncateContent(content));
        return LocalConfigInfoProcessor.getSnapshot(this, dataId, group);
    }

    
  
    public String getConfig(String dataId, String group, int feature, long timeoutMs) throws IOException{
    	if(feature == Constants.GETCONFIG_LOCAL_SERVER_SNAPSHOT){
    		return getConfig(dataId, group, timeoutMs);
    	}
    	group = null2defaultGroup(group);

        if (MockServer.isTestMode()) {
            return MockServer.getConfigInfo(dataId, group, this);
        }

        String content = LocalConfigInfoProcessor.getFailover(this, dataId, group);
        if (content != null) {
            log.warn(getName(), "[get-config] get failover ok, dataId={}, group={}, config={}", dataId, group, ContentUtils.truncateContent(content));
            return content;
        }
        content = LocalConfigInfoProcessor.getSnapshot(this, dataId, group);
        if(StringUtils.isNotEmpty(content)){
            log.warn(getName(), "[get-config] get snapshot ok, dataId={}, group={}, config={}", dataId, group, ContentUtils.truncateContent(content));
        	return content;
        }
        return ClientWorker.getServerConfig(this, dataId, group, timeoutMs);
    }
    
    public String getConfigFromSnapshot(String dataId, String group) {
    	return LocalConfigInfoProcessor.getSnapshot(this, dataId, group);
    }
    
    
	public boolean publishSingle(String dataId, String group, String content) {
		return publishSingle(dataId, group, null, content);
	}
    
	public boolean publishSingle(String dataId, String group, String appName,
			String content) {
		checkNotNull(dataId, content);
		group = null2defaultGroup(group);

		if (MockServer.isTestMode()) {
			MockServer.setConfigInfo(dataId, group, content, this);
			return true;
		}

		String url = "/basestone.do?method=syncUpdateAll";
		List<String> params = null;
		if (appName == null) {
			params = Arrays.asList("dataId", dataId, "group", group, "content",
					content);
		} else {
			params = Arrays.asList("dataId", dataId, "group", group, "appName",
					appName, "content", content);
		}

		HttpResult result = null;
		try {
			result = agent.httpPost(url, SpasAdapter.getSignHeaders(group), params, Constants.ENCODE,
					POST_TIMEOUT);
		} catch (IOException ioe) {
			log.warn("Diamond-0006", LoggerHelper.getErrorCodeStr("Diamond",
					"Diamond-0006", "环境问题", "[publish-single] exception"));
			log.warn(getName(),
					"[publish-single] exception, dataId={}, group={}, msg={}",
					dataId, group, ioe.toString());
			return false;
		}

		if (HttpURLConnection.HTTP_OK == result.code) {
			log.info(getName(),
					"[publish-single] ok, dataId={}, group={}, config={}",
					dataId, group, ContentUtils.truncateContent(content));
			return true;
		} else {
			log.warn(
					getName(),
					"[publish-single] error, dataId={}, group={}, code={}, msg={}",
					dataId, group, result.code, result.content);
			return false;
		}

	}

	
    public boolean publishAggr(String dataId, String group, String datumId, String content) {
        return publishAggr(dataId, group, datumId, null, content);
    }

    public boolean publishAggr(String dataId, String group, String datumId, String appName, String content) {
        checkNotNull(dataId, datumId, content);
        group = null2defaultGroup(group);
        String url = "/datum.do?method=addDatum";
        List<String> params = null;
		if (appName == null) {
			params = Arrays.asList("dataId", dataId, "group", group, "datumId",
					datumId, "content", content);
		} else {
			params = Arrays.asList("dataId", dataId, "group", group, "datumId",
					datumId, "appName", appName, "content", content);
		}

        HttpResult result = null;
        try {
            result = agent.httpPost(url, SpasAdapter.getSignHeaders(group), params, Constants.ENCODE, POST_TIMEOUT);
        } catch (IOException ioe) {
            log.warn(getName(), "[publish-aggr] exception, dataId={}, group={}, datumId={}, msg={}", dataId, group, datumId, ioe.toString());
            return false;
        }

        if (HttpURLConnection.HTTP_OK == result.code) {
            log.info(getName(), "[publish-aggr] ok, dataId={}, group={}, datumId={}, config={}", dataId, group, datumId, ContentUtils.truncateContent(content));
            return true;
        } else {
        	if(result.code==403){
        		log.error("Diamond-0004", LoggerHelper.getErrorCodeStr("Diamond", "Diamond-0004", "业务问题","[publish-aggr] error"));
        	}
            log.error(getName(), "[publish-aggr] error, dataId={}, group={}, code={}, msg={}", dataId, group, result.code, result.content);
            return false;
        }
    }
    public boolean removeAggr(String dataId, String group, String datumId) {
        checkNotNull(dataId, datumId);
        group = null2defaultGroup(group);
        String url = "/datum.do?method=deleteDatum";
        List<String> params = Arrays.asList("dataId", dataId, "group", group, "datumId", datumId);

        HttpResult result = null;
        try {
            result = agent.httpPost(url, SpasAdapter.getSignHeaders(group), params, Constants.ENCODE, POST_TIMEOUT);
        } catch (IOException ioe) {
            log.warn(getName(), "[remove-aggr] exception, dataId={}, group={}, datumId={}, msg={}", dataId, group, datumId, ioe.toString());
            return false;
        }

        if (HttpURLConnection.HTTP_OK == result.code) {
            log.info(getName(), "[remove-aggr] ok, dataId={}, group={}, datumId={}", dataId, group, datumId);
            return true;
        } else {
            log.error(getName(),"[remove-aggr] error, dataId={}, group={}, datumId={}, code={}, msg={}", dataId, group, datumId, result.code, result.content);
            return false;
        }
    }

    public boolean remove(String dataId, String group) {
        checkNotNull(dataId);
        group = null2defaultGroup(group);

        if (MockServer.isTestMode()) {
            MockServer.removeConfigInfo(dataId, group, this);
            return true;
        }
        
        String url = "/datum.do?method=deleteAllDatums";
        List<String> params = Arrays.asList("dataId", dataId, "group", group);

        HttpResult result = null;
        try {
            result = agent.httpPost(url, SpasAdapter.getSignHeaders(group), params, Constants.ENCODE, POST_TIMEOUT);
        } catch (IOException ioe) {
            log.warn("[remove] error, " + dataId + ", " + group + ", msg: " + ioe.toString());
            return false;
        }

        if (HttpURLConnection.HTTP_OK == result.code) {
            log.info(getName(), "[remove] ok, dataId={}, group={}", dataId, group);
            return true;
        } else {
            log.warn(getName(), "[remove] error, dataId={}, group={}, code={}, msg={}", dataId, group, result.code, result.content);
            return false;
        }
    }

    public List<String> getServerUrls() {
        return new ArrayList<String>(serverMgr.serverUrls);
    }
    
    private static void checkNotNull(String... params) {
        for (String param : params) {
            if (StringUtils.isBlank(param)) {
                throw new IllegalArgumentException("param cannot be blank");
            }
        }
    }
    
    private String null2defaultGroup(String group) {
        return (null == group) ? Constants.DEFAULT_GROUP : group.trim();
    }

   
    public BatchHttpResult<ConfigInfoEx> batchGetConfig(List<String> dataIds, String group, long timeoutMs) {
        // check parameters
        if (dataIds == null) {
            throw new IllegalArgumentException("dataId list is null when batch get config");
        }

        group = null2defaultGroup(group);

        if(MockServer.isTestMode()){
            List<ConfigInfoEx> result = MockServer.batchQuery(dataIds, group, this);
            BatchHttpResult<ConfigInfoEx> response = new BatchHttpResult<ConfigInfoEx>(true, HttpURLConnection.HTTP_OK, "", "mock server");
            response.getResult().addAll(result);
            return response;
        }

        StringBuilder dataIdstr = new StringBuilder();
        String split = "";
        for (String dataId : dataIds) {
            dataIdstr.append(split);
            dataIdstr.append(dataId);
            split = Constants.WORD_SEPARATOR;
        }

        // fire http request
        String url = "/config.co?method=batchGetConfig";
        List<String> params = Arrays.asList("dataIds", dataIdstr.toString(), "group", group);

        HttpResult result = null;
        try {
            result = agent.httpPost(url, SpasAdapter.getSignHeaders(group), params, Constants.ENCODE, timeoutMs);
        } catch (IOException ioe) { // 发送请求失败
            log.warn(getName(), "[batch-get] exception, dataIds={}, group={}, msg={}", dataIds, group, ioe);
            return new BatchHttpResult<ConfigInfoEx>(false, -1, "batch get config io exception:" + ioe.getMessage(), "");
        }

        // prepare response
        BatchHttpResult<ConfigInfoEx> response = new BatchHttpResult<ConfigInfoEx>(true, result.code, "", result.content);

        // handle http code
        if(result.code == HttpURLConnection.HTTP_OK){ // http code 200
            response.setSuccess(true);
            response.setStatusMsg("batch get config success");
            log.info(getName(), "[batch-get] ok, dataIds={}, group={}", dataIds, group);
        } else { // http code: 412 500
            response.setSuccess(false);
            response.setStatusMsg("batch get config fail, status:" + result.code);
            log.warn(getName(), "[batch-get] error, dataIds={}, group={}, code={}, msg={}", dataIds, group, result.code, result.content);
        }

        // deserialize batch query result items
        if (HttpURLConnection.HTTP_OK == result.code ||
                HttpURLConnection.HTTP_PRECON_FAILED == result.code) {
            try {
                String json = result.content;
                Object resultObj = JSONUtils.deserializeObject(json,
                        new TypeReference<List<ConfigInfoEx>>() {
                        });
                response.getResult().addAll((List<ConfigInfoEx>) resultObj);
                LocalConfigInfoProcessor.batchSaveSnapshot(this, (List<ConfigInfoEx>)resultObj);
            } catch (Exception e) { 
                response.setSuccess(false);
                response.setStatusMsg("batch get config deserialize error");
                log.warn(getName(), "[batch-get] deserialize error, dataIds={}, group={}, msg={}", dataIds, group, e.toString());
            }
        }

        return response;
    }

    @SuppressWarnings("unchecked")
    public BatchHttpResult<ConfigInfoEx> batchQuery(List<String> dataIds, String group,
            long timeoutMs) {

        BatchHttpResult<ConfigInfoEx> response = new BatchHttpResult<ConfigInfoEx>();

        if (dataIds == null) {
            throw new IllegalArgumentException("dataId list is null when batch query");
        }

        group = null2defaultGroup(group);

        if(MockServer.isTestMode()){
            List<ConfigInfoEx> result = MockServer.batchQuery(dataIds, group, this);
            response.setStatusCode(HttpURLConnection.HTTP_OK);
            response.setResponseMsg("mock server");
            response.setSuccess(true);
            response.getResult().addAll(result);
            return response;
        }

        StringBuilder dataIdstr = new StringBuilder();
        String split = "";
        for (String dataId : dataIds) {
            dataIdstr.append(split);
            dataIdstr.append(dataId);
            split = Constants.WORD_SEPARATOR;
        }

        String url = "/admin.do?method=batchQuery";
        List<String> params = Arrays.asList("dataIds", dataIdstr.toString(), "group", group);

        HttpResult result = null;
        try {
            result = agent.httpPost(url, SpasAdapter.getSignHeaders(group), params, Constants.ENCODE, timeoutMs);
        } catch (IOException ioe) {
            log.warn(getName(), "[batch-query] exception, dataIds={}, group={}, msg={}", dataIds, group, ioe);
            response.setSuccess(false);
            response.setStatusMsg("batch query io exception：" + ioe.getMessage());
            return response;
        }

        response.setStatusCode(result.code);
        response.setResponseMsg(result.content);

        if (HttpURLConnection.HTTP_OK == result.code ||
                HttpURLConnection.HTTP_PRECON_FAILED == result.code) {

            try {
                String json = result.content;
                Object resultObj = JSONUtils.deserializeObject(json,
                        new TypeReference<List<ConfigInfoEx>>() {
                        });
                response.setSuccess(true);
                response.getResult().addAll((List<ConfigInfoEx>) resultObj);
                log.info(getName(), "[batch-query] ok, dataIds={}, group={}", dataIds, group);
            } catch (Exception e) {
                response.setSuccess(false);
                response.setStatusMsg("batch query deserialize error");
                log.warn(getName(), "[batch-query] deserialize error, dataIds={}, group={}, msg={}", dataIds, group, e.toString());
            }


        } else {
            response.setSuccess(false);
            response.setStatusMsg("batch query fail, status:" + result.code);
            log.warn(getName(), "[batch-query] error, dataIds={}, group={}, code={}, msg={}", dataIds, group, result.code, result.content);
            return response;

        }

        return response;
    }
    

    public boolean batchRemoveAggr(String dataId, String group, List<String> datumIdList, long timeoutMs) {
    	checkNotNull(dataId, group);
    	if(datumIdList == null || datumIdList.isEmpty()){
    		throw new IllegalArgumentException("datumIdList cannot be blank"); 
    	}
    	StringBuilder datumStr = new StringBuilder();
    	for(String datum : datumIdList){
    		datumStr.append(datum).append(Constants.WORD_SEPARATOR);
    	}
    	String url = "/datum.do?method=batchDeleteAggrs";
    	List<String> params = Arrays.asList("dataId", dataId, "group", group, "datumList", datumStr.toString());
    	HttpResult result = null;
        try {
            result = agent.httpPost(url, SpasAdapter.getSignHeaders(group), params, Constants.ENCODE, timeoutMs);
            if(result.code == HttpURLConnection.HTTP_OK){
            	return true;
            } else {
                log.warn("response code :"+result.code + ", error message :" + result.content);
            }
        } catch (IOException ioe) {
            log.warn(getName(), "[batchRemoveAggr] exception, dataId{}, group={}, msg={}", dataId, group, ioe);
        }
        return false;
    }
    

    public boolean batchPublishAggr(String dataId, String group, Map<String, String> datumMap, long timeoutMs){
    	return batchPublishAggr(dataId, group, datumMap, null, timeoutMs);
    }
    

    public boolean batchPublishAggr(String dataId, String group, Map<String, String> datumMap, String appName, long timeoutMs){
    	checkNotNull(dataId, group);
    	if(datumMap == null || datumMap.isEmpty()){
    		throw new IllegalArgumentException("datumMap cannot be blank"); 
    	}
    	StringBuilder datumStr = new StringBuilder();
    	for(Entry<String, String> datumEntry : datumMap.entrySet()){
    		datumStr.append(datumEntry.getKey()).append(Constants.WORD_SEPARATOR).append(datumEntry.getValue()).append(Constants.LINE_SEPARATOR);
    	}
		String url = "/datum.do?method=batchAddAggrs";
		List<String> params = null;
		if (appName == null) {
			params = Arrays.asList("dataId", dataId, "group", group, "datas",
					datumStr.toString());
		} else {
			params = Arrays.asList("dataId", dataId, "group", group, "datas",
					datumStr.toString(), "appName", appName);
		}
    	HttpResult result = null;
    	try {
    		result = agent.httpPost(url, SpasAdapter.getSignHeaders(group), params, Constants.ENCODE, timeoutMs);
    		if(result.code == HttpURLConnection.HTTP_OK){
    			log.info(getName(),
    					"[batchPublishAggr] ok, dataId={}, group={}", dataId,
    					group);
    			return true;
    		} else {
    			log.warn("response code :"+result.code + ", error message :" + result.content);
    		}
    	} catch (IOException ioe) {
    		log.warn(getName(), "[batchPublishAggr] exception, dataId{}, group={}, msg={}", dataId, group, ioe);
    	}
    	return false;
    }
    

    public boolean replaceAggr(String dataId, String group, Map<String, String> datumMap, long timeoutMs) {
    	return replaceAggr(dataId, group, datumMap, null, timeoutMs);
    }

    public boolean replaceAggr(String dataId, String group, Map<String, String> datumMap, String appName, long timeoutMs) {
    	checkNotNull(dataId, group);
    	if(datumMap == null || datumMap.isEmpty()){
    		throw new IllegalArgumentException("datumMap cannot be blank"); 
    	}
    	StringBuilder datumStr = new StringBuilder();
    	for(Entry<String, String> datumEntry : datumMap.entrySet()){
    		datumStr.append(datumEntry.getKey()).append(Constants.WORD_SEPARATOR).append(datumEntry.getValue()).append(Constants.LINE_SEPARATOR);
    	}
    	String url = "/datum.do?method=replaceAggr";
    	List<String> params  = null;
    	if (appName == null) {
    		params = Arrays.asList("dataId", dataId, "group", group, "datas", datumStr.toString());
		} else {
			params = Arrays.asList("dataId", dataId, "group", group, "datas",
					datumStr.toString(), "appName", appName);
		}
    	
    	HttpResult result = null;
    	try {
    		result = agent.httpPost(url, SpasAdapter.getSignHeaders(group), params, Constants.ENCODE, timeoutMs);
    		if(result.code == HttpURLConnection.HTTP_OK){
    			return true;
    		} else{
    			log.warn("response code :"+result.code + ", error message :" + result.content);
    		}
    	} catch (IOException ioe) {
    		log.warn(getName(), "[replaceAggr] exception, dataId{}, group={}, msg={}", dataId, group, ioe);
    	}
    	return false;
    }
    
    CacheData getCache(String dataId, String group) {
        if (null == dataId || null == group) {
            throw new IllegalArgumentException();
        }
        return cacheMap.get().get(GroupKey.getKey(dataId, group));
    }
    
    List<CacheData> getAllCacheDataSnapshot() {
        return new ArrayList<CacheData>(cacheMap.get().values());
    }
    
    void removeCache(String dataId, String group) {
        String groupKey = GroupKey.getKey(dataId, group);
        synchronized (cacheMap) {
            Map<String, CacheData> copy = new HashMap<String, CacheData>(cacheMap.get());
            copy.remove(groupKey);
            cacheMap.set(copy);
        }
        log.info(getName(), "[unsubscribe] {}", groupKey);
    }
    
    public CacheData addCacheDataIfAbsent(String dataId, String group) {
        CacheData cache = getCache(dataId, group);
        if (null != cache) {
            return cache;
        }

        synchronized (cacheMap) {
            String key = GroupKey.getKey(dataId, group);
            cache = new CacheData(this, dataId, group);

            Map<String, CacheData> copy = new HashMap<String, CacheData>(cacheMap.get());
            copy.put(key, cache);
            cacheMap.set(copy);
            log.info(getName(), "[subscribe] {}", key);
        }

		String content = LocalConfigInfoProcessor.getFailover(this, dataId, group);
		cache.setContent(content);
        return cache;
    }
    
    public Set<String> getSubscribeDataIds() {
        Map<String, CacheData> cacheMapSnapshot = cacheMap.get();
        
        Set<String> dataIds = new HashSet<String>(cacheMapSnapshot.size());
        for (CacheData cache : cacheMapSnapshot.values()) {
            dataIds.add(cache.dataId);
        }
        return dataIds;
    }
    
    @Override
    public String toString() {
        return "DiamondEnv-" + serverMgr.toString();
    }

    public ServerListManager getServerMgr() {
        return serverMgr;
    }

    public String getName() {
        return serverMgr.name;
    }

    public void initServerManager(ServerListManager _serverMgr) {
    	_serverMgr.setEnv(this);
        serverMgr = _serverMgr;
        serverMgr.start();
        agent = new ServerHttpAgent(serverMgr);
    }

    public DiamondEnv(String... serverIps) {
        this(new ServerListManager(Arrays.asList(serverIps)));
    }
    
    protected DiamondEnv(ServerListManager serverListMgr) {
        initServerManager(serverListMgr);
        cacheMap = new AtomicReference<Map<String, CacheData>>(new HashMap<String, CacheData>());
        worker = new ClientWorker(this);
    }
    
    // =====================

    static final public Logger log = LogUtils.logger(DiamondEnv.class);
    static public final long POST_TIMEOUT = 3000L;
        
    protected ServerListManager serverMgr;
    protected ServerHttpAgent agent; 
    protected ClientWorker worker ;
    
    final private AtomicReference<Map<String/* groupKey */, CacheData>> cacheMap;

}
