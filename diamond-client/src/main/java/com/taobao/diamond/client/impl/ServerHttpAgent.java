package com.taobao.diamond.client.impl;

import com.taobao.diamond.client.impl.HttpSimpleClient.HttpResult;
import com.taobao.diamond.utils.AppNameUtils;
import com.taobao.middleware.logger.support.LoggerHelper;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.taobao.diamond.client.impl.DiamondEnv.log;

public class ServerHttpAgent {
    public static final String appKey;
    public static final String appName;
    public static String serverPort;
    public static String domainName;
    public static String addressPort;

    static {
        // 客户端身份信息
        appKey = System.getProperty("diamond.client.appKey", "");
        appName = AppNameUtils.getAppName();
        
        serverPort = System.getProperty("diamond.server.port", "8080");
        
        domainName =  System.getProperty("address.server.domain", "jmenv.tbsite.net");
        
        addressPort = System.getProperty("address.server.port", "8080");

		log.info("settings",
				"address-server domain:{} ,address-server port:{}", domainName,
				addressPort);
    }

    ServerHttpAgent(ServerListManager mgr) {
        serverListMgr = mgr;
    }

    /**
     * @param path
     *            相对于web应用根，以/开头
     * @param headers
     * @param paramValues
     * @param encoding
     * @param readTimeoutMs
     * @return
     * @throws IOException
     */
    public HttpResult httpGet(String path, List<String> headers, List<String> paramValues,
            String encoding, long readTimeoutMs) throws IOException {
        final long endTime = System.currentTimeMillis() + readTimeoutMs;
        String port = "8080";
        List<String> newHeaders = new ArrayList<String>();
        newHeaders.add("Spas-AccessKey");
        newHeaders.add(SpasAdapter.getAk());
        
        if(headers != null){
        	newHeaders.addAll(headers);
        }
        if (null != currentServerIp) {
            try {
				port = serverListMgr.getPortByIp(currentServerIp);
				HttpResult result = HttpSimpleClient.httpGet(
						getUrl(currentServerIp, port, path),
                        newHeaders, paramValues, encoding, readTimeoutMs);
                return result;
            } catch (ConnectException ce) {
            	log.error("Diamond ConnectException",
						"currentServerIp:{},port:{}", new Object[] {
								currentServerIp, port });
            } catch (SocketTimeoutException stoe) {
            	log.error("Diamond  SocketTimeoutException",
						"currentServerIp:{},port:{}", new Object[] {
								currentServerIp, port });
            } catch (IOException ioe) {
            	log.error("Diamond  IOException", "currentServerIp:{},port:{}",
						new Object[] { currentServerIp, port });
                throw ioe;
            }
        }

        for (Iterator<String> serverIter = serverListMgr.iterator(); serverIter.hasNext();) {
            long timeout = endTime - System.currentTimeMillis();
            if (timeout <= 0) {
            	if(null!= currentServerIp){
            		log.error("the currentServerIp  which happened IOException in get(timeout) is: ", currentServerIp);
            	}
                currentServerIp = serverIter.next(); // previous node performs slowly
                //log.info("the currentServerIp  in get() after serverIter.next is: ", currentServerIp);
                throw new IOException("timeout");
            }

            String ip = serverIter.next();
            try {
                port = serverListMgr.getPortByIp(ip);
                HttpResult result = HttpSimpleClient.httpGet(getUrl(ip, port, path), newHeaders,
                        paramValues, encoding, timeout);
                currentServerIp = ip;
                // log.info("the currentServerIp in get() is: ", currentServerIp);
                return result;
            } catch (ConnectException ce) {
            	log.error("Diamond ConnectException",
						"currentServerIp:{},port:{}", new Object[] {
								currentServerIp, port });
            } catch (SocketTimeoutException stoe) {
            	log.error("Diamond  SocketTimeoutException",
						"currentServerIp:{},port:{}", new Object[] {
								currentServerIp, port });
            } catch (IOException ioe) {
            	log.error("Diamond  IOException", "currentServerIp:{},port:{}",
						new Object[] { currentServerIp, port });
                throw ioe;
            }
        }
        log.error("Diamond-0002", LoggerHelper.getErrorCodeStr("Diamond", "Diamond-0002", "环境问题","no available server"));
        throw new ConnectException("no available server");
    }

    public HttpResult httpPost(String path, List<String> headers, List<String> paramValues,
            String encoding, long readTimeoutMs) throws IOException {
        final long endTime = System.currentTimeMillis() + readTimeoutMs;
        
        List<String> newHeaders = new ArrayList<String>();
        newHeaders.add("Spas-AccessKey");
        newHeaders.add(SpasAdapter.getAk());
        if(headers != null){
        	newHeaders.addAll(headers);
        }
        String port = "8080";
        if (null != currentServerIp) {
            try {
            	port = serverListMgr.getPortByIp(currentServerIp);
                HttpResult result = HttpSimpleClient.httpPost(getUrl(currentServerIp, port, path),
                        newHeaders, paramValues, encoding, readTimeoutMs);
                return result;
            } catch (ConnectException ce) {
				log.error("Diamond ConnectException",
						"currentServerIp:{},port:{}", new Object[] {
								currentServerIp, port });
            } catch (SocketTimeoutException stoe) {
				log.error("Diamond  SocketTimeoutException",
						"currentServerIp:{},port:{}", new Object[] {
								currentServerIp, port });
            } catch (IOException ioe) {
				log.error("Diamond  IOException", "currentServerIp:{},port:{}",
						new Object[] { currentServerIp, port });
                throw ioe;
            }
        }

        for (Iterator<String> serverIter = serverListMgr.iterator(); serverIter.hasNext();) {
            long timeout = endTime - System.currentTimeMillis();
            if (timeout <= 0) {
            	if(null!= currentServerIp){
            		log.error("the currentServerIp  which happened IOException(timeout) in post is: ", currentServerIp);
            	}
                currentServerIp = serverIter.next(); // previous node performs slowly
               // log.info("the currentServerIp in post() after serverIter.next is: ", currentServerIp);
                throw new IOException("timeout");
            }

            String ip = serverIter.next();
            try {
            	port = serverListMgr.getPortByIp(ip);
                HttpResult result = HttpSimpleClient.httpPost(getUrl(ip, port, path), newHeaders,
                        paramValues, encoding, timeout);
                currentServerIp = ip;
                //log.info("the currentServerIp in post is: ", currentServerIp);
                return result;
            } catch (ConnectException ce) {
            	log.error("Diamond ConnectException",
						"currentServerIp:{},port:{}", new Object[] {
								currentServerIp, port });
            } catch (SocketTimeoutException stoe) {
            	log.error("Diamond  SocketTimeoutException",
						"currentServerIp:{},port:{}", new Object[] {
								currentServerIp, port });
            } catch (IOException ioe) {
            	log.error("Diamond  IOException", "currentServerIp:{},port:{}",
						new Object[] { currentServerIp, port });
                throw ioe;
            }
        }
        log.error("Diamond-0002", LoggerHelper.getErrorCodeStr("Diamond", "Diamond-0002", "环境问题","no available server"));
        throw new ConnectException("no available server");
    }

    // relativePath相对于web应用根路径，以/开头
	static String getUrl(String ip, String port, String relativePath) {
		return "http://" + ip + ":" + port + "/diamond-server" + relativePath;
	}
    
    /**
     * 集群服务器列表发送变化，重置currentServerIp
     */
    public void reSetCurrentServerIp(){
    	if(currentServerIp!=null)
    		currentServerIp = null;
    }

    // =================
    final ServerListManager serverListMgr;
    volatile String currentServerIp;
}
