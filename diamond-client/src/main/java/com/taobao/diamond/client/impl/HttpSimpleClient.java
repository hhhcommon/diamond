package com.taobao.diamond.client.impl;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.md5.MD5;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import static com.taobao.diamond.client.impl.DiamondEnv.log;

public class HttpSimpleClient {

    static String DIAMOND_CLIENT_VERSION = "unknown";

    static final int DIAMOND_CONNECT_TIMEOUT;

    static {
        String tmp = "1000"; //change timeout from 100 to 200
        try {
            tmp = System.getProperty("DIAMOND.CONNECT.TIMEOUT","1000"); //change timeout from 100 to 200
            DIAMOND_CONNECT_TIMEOUT = Integer.parseInt(tmp);
        } catch (NumberFormatException e) {
            final String msg = "[http-client] invalid connect timeout:" + tmp;
            log.error("settings", "DIAMOND-XXXX", msg, e);
            throw new IllegalArgumentException(msg, e);
        }
        log.info("settings","[http-client] connect timeout:{}", DIAMOND_CONNECT_TIMEOUT);
        
		try {
			URL configURL = HttpSimpleClient.class
					.getResource("/application.properties");
			if (configURL != null) {
				URI configURI = configURL.toURI();
				File file = new File(configURI);
				Properties props = new Properties();
				props.load(new FileInputStream(file));
				String val = null;
				val = props.getProperty("version");
				if (val != null) {
					DIAMOND_CLIENT_VERSION = val;
				}
				log.info("DIAMOND_CLIENT_VERSION:{}", DIAMOND_CLIENT_VERSION);
			} else {
				log.error("500", "configURL is null");
			}
		} catch (Exception e) {
			log.error("500", "read config.properties wrong", e);
		}
		
    }
    static public HttpResult httpGet(String url, List<String> headers, List<String> paramValues,
            String encoding, long readTimeoutMs) throws IOException {
        String encodedContent = encodingParams(paramValues, encoding);
        url += (null == encodedContent) ? "" : ("?" + encodedContent);
        
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(DIAMOND_CONNECT_TIMEOUT > 100 ? DIAMOND_CONNECT_TIMEOUT : 100);
            conn.setReadTimeout((int) readTimeoutMs);
            setHeaders(conn, headers, encoding);

            conn.connect();
            int respCode = conn.getResponseCode(); 
            String resp = null;

            if (HttpURLConnection.HTTP_OK == respCode) {
                resp = IOUtils.toString(conn.getInputStream(), encoding);
            } else {
                resp = IOUtils.toString(conn.getErrorStream(), encoding);
            }
            return new HttpResult(respCode, resp);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }


    static public HttpResult httpPost(String url, List<String> headers, List<String> paramValues,
            String encoding, long readTimeoutMs) throws IOException {
        String encodedContent = encodingParams(paramValues, encoding);
        
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(DIAMOND_CONNECT_TIMEOUT > 3000 ? DIAMOND_CONNECT_TIMEOUT : 3000);
            conn.setReadTimeout((int) readTimeoutMs);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            setHeaders(conn, headers, encoding);

            conn.getOutputStream().write(encodedContent.getBytes());

            int respCode = conn.getResponseCode(); 
            String resp = null;

            if (HttpURLConnection.HTTP_OK == respCode) {
                resp = IOUtils.toString(conn.getInputStream(), encoding);
            } else {
                resp = IOUtils.toString(conn.getErrorStream(), encoding);
            }
            return new HttpResult(respCode, resp);
        } finally {
            if (null != conn) {
                conn.disconnect();
            }
        }
    }

    static private void setHeaders(HttpURLConnection conn, List<String> headers, String encoding) {
        if (null != headers) {
            for (Iterator<String> iter = headers.iterator(); iter.hasNext();) {
                conn.addRequestProperty(iter.next(), iter.next());
            }
        }
        conn.addRequestProperty("Client-Version", DIAMOND_CLIENT_VERSION); // TODO
        conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset="
                + encoding);

        //
        String ts = String.valueOf(System.currentTimeMillis());
        String token = MD5.getInstance().getMD5String(ts + ServerHttpAgent.appKey);

        conn.addRequestProperty(Constants.CLIENT_APPNAME_HEADER, ServerHttpAgent.appName);
        conn.addRequestProperty(Constants.CLIENT_REQUEST_TS_HEADER, ts);
        conn.addRequestProperty(Constants.CLIENT_REQUEST_TOKEN_HEADER, token);
    }

    static private String encodingParams(List<String> paramValues, String encoding)
            throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        if (null == paramValues) {
            return null;
        }
        
        for (Iterator<String> iter = paramValues.iterator(); iter.hasNext();) {
            sb.append(iter.next()).append("=");
            sb.append(URLEncoder.encode(iter.next(), encoding));
            if (iter.hasNext()) {
                sb.append("&");
            }
        }
        return sb.toString();
    }
    
    
    
    static public class HttpResult {
        final public int code;
        final public String content;

        public HttpResult(int code, String content) {
            this.code = code;
            this.content = content;
        }
    }
}
