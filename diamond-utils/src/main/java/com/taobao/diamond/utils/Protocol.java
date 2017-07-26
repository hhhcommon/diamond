package com.taobao.diamond.utils;

/**
 * 
 * @author zhidao
 * @version 1.0 2011/05/03
 * 
 */
public class Protocol {
    public static int getVersionNumber(String version) {
        if (version == null)
            return -1;

        String[] vs = version.split("\\.");
        int sum = 0;
        for (int i = 0; i < vs.length; i++) {
            try {
                sum = sum * 10 + Integer.parseInt(vs[i]);
            }
            catch (Exception e) {
                // ignore
            }
        }
        return sum;
    }
}
