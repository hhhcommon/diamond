package com.taobao.diamond.client.impl;

import com.taobao.diamond.client.Constant;
import com.taobao.diamond.client.impl.HttpSimpleClient.HttpResult;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.common.GroupKey;
import com.taobao.diamond.md5.MD5;
import com.taobao.diamond.mockserver.MockServer;
import com.taobao.diamond.utils.ContentUtils;
import com.taobao.diamond.utils.StringUtils;
import com.taobao.middleware.logger.support.LoggerHelper;
import com.taobao.spas.sdk.client.SpasSdkClientFacade;
import com.taobao.spas.sdk.common.sign.SpasSigner;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.taobao.diamond.client.impl.DiamondEnv.log;
import static com.taobao.diamond.common.Constants.LINE_SEPARATOR;
import static com.taobao.diamond.common.Constants.WORD_SEPARATOR;



public class ClientWorker {
    
    static String getServerConfig(DiamondEnv env, String dataId, String group, long readTimeout)
            throws IOException {
        if (StringUtils.isBlank(group)) {
            group = Constants.DEFAULT_GROUP;
        }


        if (MockServer.isTestMode()) {
            return MockServer.getConfigInfo(dataId, group, env);
        }

        HttpResult result = null;
        try {
            List<String> params = Arrays.asList("dataId", dataId, "group", group);
            result = env.agent.httpGet("/config.co", SpasAdapter.getSignHeaders(group), params, Constants.ENCODE, readTimeout);
        } catch (IOException e) {
            log.error(env.getName(), "DIAMOND-XXXX", "[sub-server] get server config exception, dataId={}, group={}, msg={}", dataId, group, e.toString());
            throw e;
        }

        switch (result.code) {
		case HttpURLConnection.HTTP_OK:
			LocalConfigInfoProcessor.saveSnapshot(env, dataId, group, result.content);
			return result.content;
		case HttpURLConnection.HTTP_NOT_FOUND:
			LocalConfigInfoProcessor.saveSnapshot(env, dataId, group, null);
            return null;
        case HttpURLConnection.HTTP_CONFLICT: {
            log.error(env.getName(), "DIAMOND-XXXX", "[sub-server-error] get server config being modified concurrently, dataId={}, group={}", dataId, group);
            throw new IOException("data being modified, dataId=" + dataId + ",group=" + group);
        }
        case HttpURLConnection.HTTP_FORBIDDEN:
        {
        	log.error(env.getName(), "DIAMOND-XXXX", "[sub-server-error] no right, dataId={}, group={}", dataId, group);
            throw new IOException(Constant.NO_RIGHT);
        }
        default: {
            log.error(env.getName(), "DIAMOND-XXXX", "[sub-server-error]  dataId={}, group={}, code={}", dataId, group, result.code);
            throw new IOException("http error, code=" + result.code + ",dataId=" + dataId + ",group=" + group);
        }
        }
    }

    private void checkLocalConfigInfo() {
        for (CacheData cacheData : env.getAllCacheDataSnapshot()) {
            try {
                checkLocalConfig(env, cacheData);
            } catch (Exception e) {
                log.error("DIAMOND-CLIENT","get local config info error", e);
            }
        }
        checkListenerMd5(env);
    }
    
	
    static void checkLocalConfig(DiamondEnv env, CacheData cacheData) {
        final String dataId = cacheData.dataId;
        final String group = cacheData.group;
        File path = LocalConfigInfoProcessor.getFailoverFile(env, dataId, group);

        if (!cacheData.isUseLocalConfigInfo() && path.exists()) {
            String content = LocalConfigInfoProcessor.getFailover(env, dataId, group);
            String md5 = MD5.getInstance().getMD5String(content);
            cacheData.setUseLocalConfigInfo(true);
            cacheData.setLocalConfigInfoVersion(path.lastModified());
            cacheData.setContent(content);

            log.warn(env.getName(), "[failover-change] failover file created. dataId={}, group={}, md5={}, content={}", dataId, group, md5, ContentUtils.truncateContent(content));
            return;
        }

        if (cacheData.isUseLocalConfigInfo() && !path.exists()) {
            cacheData.setUseLocalConfigInfo(false);

            log.warn(env.getName(), "[failover-change] failover file deleted. dataId={}, group={}", dataId, group);
            return;
        }

        if (cacheData.isUseLocalConfigInfo() && path.exists()
                && cacheData.getLocalConfigInfoVersion() != path.lastModified()) {
            String content = LocalConfigInfoProcessor.getFailover(env, dataId, group);
            String md5 = MD5.getInstance().getMD5String(content);
            cacheData.setUseLocalConfigInfo(true);
            cacheData.setLocalConfigInfoVersion(path.lastModified());
            cacheData.setContent(content);

            log.warn(env.getName(), "[failover-change] failover file changed. dataId={}, group={}, md5={}, content={}", dataId, group, md5, ContentUtils.truncateContent(content));
            return;
        }
    }

	public void checkServerConfigInfo() {
		checkServerConfigInfo(env);
	}
    
    static public void checkServerConfigInfo(DiamondEnv env) {
        List<String> changedGroupKeys = checkUpdateDataIds(env);

        for (String groupKey : changedGroupKeys) {
            String dataId = GroupKey.parseKey(groupKey)[0];
            String group = GroupKey.parseKey(groupKey)[1];
            try {
                String content = getServerConfig(env, dataId, group, 3000L);
                CacheData cache = env.getCache(dataId, group);
                cache.setContent(content);
                log.info(env.getName(), "[data-received] dataId={}, group={}, md5={}, content={}", dataId, group, cache.getMd5(), ContentUtils.truncateContent(content));
            } catch (IOException ioe) {
                log.error(env.getName(), "DIAMOND-XXXX", "[get-update] get changed config exception. dataId={}, group={}, msg={}", dataId, group, ioe.toString());
            }
        }
        checkListenerMd5(env);
    }

    static List<String> checkUpdateDataIds(DiamondEnv env) {
        if (MockServer.isTestMode()) {
            try {
                Thread.sleep(3000l);
            } catch (InterruptedException e) {}
            List<String> updateList = new ArrayList<String>();
            for(CacheData cacheData : env.getAllCacheDataSnapshot()){
                if(!CacheData.getMd5String(MockServer.getConfigInfo(cacheData.dataId, cacheData.group, env))
                        .equals(cacheData.getMd5())) {
                    updateList.add(GroupKey.getKey(cacheData.dataId, cacheData.group));
                }
            }
            return updateList;
        }


        String probeUpdateString = getProbeUpdateString(env);
        List<String> params = Arrays.asList(Constants.PROBE_MODIFY_REQUEST, probeUpdateString);
        long timeout = TimeUnit.SECONDS.toMillis(30L);

        List<String> headers = Arrays.asList("longPullingTimeout", "" + timeout);

        if (StringUtils.isBlank(probeUpdateString)) {
            return Collections.emptyList();
        }

        try {
            HttpResult result = env.agent.httpPost("/config.co", headers, params, Constants.ENCODE,
                    timeout);

            if (HttpURLConnection.HTTP_OK == result.code) {
                return parseUpdateDataIdResponse(env, result.content);
            } else {
            	if(result.code==500){
            		log.error("Diamond-0007", LoggerHelper.getErrorCodeStr("Diamond", "Diamond-0007", "环境问题","[check-update] get changed dataId error"));
            	}
                log.error(env.getName(), "DIAMOND-XXXX", "[check-update] get changed dataId error, code={}", result.code);
            }
        } catch (IOException e) {
            log.error(env.getName(), "DIAMOND-XXXX", "[check-update] get changed dataId exception, msg={}", e.toString());
        }
        return Collections.emptyList();
    }

    static private String getProbeUpdateString(DiamondEnv env) {
        StringBuilder sb = new StringBuilder();
        for (CacheData cacheData : env.getAllCacheDataSnapshot()) {
            if (!cacheData.isUseLocalConfigInfo()) {
            	if(cacheData.isInitializing()){
            		cacheData.setContent(LocalConfigInfoProcessor.getSnapshot(env, cacheData.dataId, cacheData.group));
            		cacheData.setInitializing(false);
            	}
                sb.append(cacheData.dataId).append(WORD_SEPARATOR);
                sb.append(cacheData.group).append(WORD_SEPARATOR);
                sb.append(cacheData.getMd5()).append(LINE_SEPARATOR);
            }
        }
        return sb.toString();
    }

    static private List<String> parseUpdateDataIdResponse(DiamondEnv env, String response) {
        if (StringUtils.isBlank(response)) {
            return Collections.emptyList();
        }

        try {
            response = URLDecoder.decode(response, "UTF-8");
        } catch (Exception e) {
            log.error(env.getName(), "DIAMOND-XXXX","[polling-resp] decode modifiedDataIdsString error", e);
        }

        List<String> updateList = new LinkedList<String>();

        for (String dataIdAndGroup : response.split(LINE_SEPARATOR)) {
            if (!StringUtils.isBlank(dataIdAndGroup)) {
                int idx = dataIdAndGroup.indexOf(WORD_SEPARATOR);
                if (idx > 0) {
                    String dataId = dataIdAndGroup.substring(0, idx);
                    String group = dataIdAndGroup.substring(idx + 1);
                    updateList.add(GroupKey.getKey(dataId, group));
                    log.info(env.getName(), "[polling-resp] config changed. dataId={}, group={}", dataId, group);
                }
            }
        }
        return updateList;
    }
    
    static void checkListenerMd5(DiamondEnv env) {
        for (CacheData cacheData : env.getAllCacheDataSnapshot()) {
            cacheData.checkListenerMd5();
        }
    }


    ClientWorker(final DiamondEnv env) {
    	this.env = env;
        executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("com.taobao.diamond.client.Worker."+ env.serverMgr.name);
                t.setDaemon(true);
                return t;
            }
        });

        executor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                try {
                    checkLocalConfigInfo();
                    checkServerConfigInfo();
                } catch (Throwable e) {
                    log.error(env.getName(), "DIAMOND-XXXX", "[sub-check] rotate check error", e);
                }
            }
        }, 1L, 1L, TimeUnit.MILLISECONDS);
    }

    // =================

    final ScheduledExecutorService executor;
    final DiamondEnv env;
}
