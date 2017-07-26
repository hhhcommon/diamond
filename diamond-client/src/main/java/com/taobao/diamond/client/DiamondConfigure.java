package com.taobao.diamond.client;

import static com.taobao.diamond.client.impl.DiamondEnv.log;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.taobao.diamond.client.impl.ServerHttpAgent;
import com.taobao.diamond.common.Constants;


public class DiamondConfigure {

    public DiamondConfigure(ClusterType clusterType) {
        initSystemProperty();
    }
    
    private DiamondConfigure() {
        initSystemProperty();
    }

    private void initSystemProperty() {
        try {
            String pollingIntervaStr = System.getProperty("diamond.polling.interval");
            if (pollingIntervaStr != null) {
                this.pollingIntervalTime = Integer.parseInt(pollingIntervaStr);
            }
            log.warn("diamond polling interval:" + this.pollingIntervalTime + "s");
        }
        catch (Exception e) {
            log.warn("parse system property error - diamond.polling.interval, use default:" + this.pollingIntervalTime
                    + "s," + e.getMessage());
        }

        try {
            String httpMaxConns = System.getProperty("diamond.http.maxhostconn");
            if (httpMaxConns != null) {
                this.maxHostConnections = Integer.parseInt(httpMaxConns);
            }
            log.warn("diamond max host conn:" + this.maxHostConnections);
        }
        catch (Exception e) {
            log.warn("parse system property error - diamond.http.maxhostconn, use default:" + this.maxHostConnections
                    + "," + e.getMessage());
        }

        try {
            String httpTotalConns = System.getProperty("diamond.http.maxtotalconn");
            if (httpTotalConns != null) {
                this.maxTotalConnections = Integer.parseInt(httpTotalConns);
            }
            log.warn("diamond max total conn:" + this.maxTotalConnections);
        }
        catch (Exception e) {
            log.warn("parse system property error - diamond.http.maxtotalconn, use default:" + this.maxTotalConnections
                    + "," + e.getMessage());
        }
    }


    public int getMaxHostConnections() {
        return maxHostConnections;
    }

    public void setMaxHostConnections(int maxHostConnections) {
        this.maxHostConnections = maxHostConnections;
    }


    public boolean isConnectionStaleCheckingEnabled() {
        return connectionStaleCheckingEnabled;
    }


    public void setConnectionStaleCheckingEnabled(boolean connectionStaleCheckingEnabled) {
        this.connectionStaleCheckingEnabled = connectionStaleCheckingEnabled;
    }


    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }


    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }


    public int getPollingIntervalTime() {
        return pollingIntervalTime;
    }


    public void setPollingIntervalTime(int pollingIntervalTime) {
        if (pollingIntervalTime < Constants.POLLING_INTERVAL_TIME) {
            return;
        }
        this.pollingIntervalTime = pollingIntervalTime;
    }


    public List<String> getDomainNameList() {
        return Arrays.asList(ServerHttpAgent.domainName);
    }


    @Deprecated
    public void setDomainNameList(List<String> domainNameList) {
        if (null == domainNameList) {
            throw new NullPointerException();
        }
        this.domainNameList = new LinkedList<String>(domainNameList);
    }



    @Deprecated
    public void addDomainName(String domainName) {
        if (null == domainName) {
            throw new NullPointerException();
        }
        this.domainNameList.add(domainName);
    }


 
    @Deprecated
    public void addDomainNames(Collection<String> domainNameList) {
        if (null == domainNameList) {
            throw new NullPointerException();
        }
        this.domainNameList.addAll(domainNameList);
    }


    public int getPort() {
        return port;
    }



    @Deprecated
    public void setPort(int port) {
        this.port = port;
    }


    public int getOnceTimeout() {
        return onceTimeout;
    }



    public void setOnceTimeout(int onceTimeout) {
        this.onceTimeout = onceTimeout;
    }



    public int getConnectionTimeout() {
        return connectionTimeout;
    }


    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }



    public int getSoTimeout() {
        return soTimeout;
    }


    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }


    @Deprecated
    public void setFilePath(String filePath) {
    }
    

    @Deprecated
    public void setReceiveWaitTime(int receiveWaitTime) {
    }
    
    @Deprecated
    public void setTotalTimeout(long totalTimeout) {
    }
    
    // ======================
    
    static final public DiamondConfigure singleton = new DiamondConfigure();
    

    private volatile int pollingIntervalTime = 5;
    private volatile List<String> domainNameList = new LinkedList<String>();

    private boolean connectionStaleCheckingEnabled = true;
    private int maxHostConnections = 20;
    private int maxTotalConnections = 50;

    private int soTimeout = Constants.SO_TIMEOUT;
    private int connectionTimeout = Constants.CONN_TIMEOUT;
    private volatile int onceTimeout = Constants.ONCE_TIMEOUT;
    
    private int port = Constants.DEFAULT_PORT;
    
}
