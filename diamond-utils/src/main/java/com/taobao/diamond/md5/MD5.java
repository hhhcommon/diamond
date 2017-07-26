package com.taobao.diamond.md5;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import com.taobao.diamond.common.Constants;


public class MD5 {
    private static char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static Map<Character, Integer> rDigits = new HashMap<Character, Integer>(16);
    static {
        for (int i = 0; i < digits.length; ++i) {
            rDigits.put(digits[i], i);
        }
    }

    private static MD5 me = new MD5();
    private MessageDigest mHasher;
    private ReentrantLock opLock = new ReentrantLock();


    private MD5() {
        try {
            mHasher = MessageDigest.getInstance("md5");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    public static MD5 getInstance() {
        return me;
    }


    public String getMD5String(String content) {
        return bytes2string(hash(content));
    }


    public String getMD5String(byte[] content) {
        return bytes2string(hash(content));
    }


    public byte[] getMD5Bytes(byte[] content) {
        return hash(content);
    }


    public byte[] hash(String str) {
        opLock.lock();
        try {
            byte[] bt = mHasher.digest(str.getBytes(Constants.ENCODE));
            if (null == bt || bt.length != 16) {
                throw new IllegalArgumentException("md5 need");
            }
            return bt;
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException("unsupported utf-8 encoding", e);
        }
        finally {
            opLock.unlock();
        }
    }



    public byte[] hash(byte[] data) {
        opLock.lock();
        try {
            byte[] bt = mHasher.digest(data);
            if (null == bt || bt.length != 16) {
                throw new IllegalArgumentException("md5 need");
            }
            return bt;
        }
        finally {
            opLock.unlock();
        }
    }



    public String bytes2string(byte[] bt) {
        int l = bt.length;

        char[] out = new char[l << 1];

        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = digits[(0xF0 & bt[i]) >>> 4];
            out[j++] = digits[0x0F & bt[i]];
        }

        return new String(out);
    }



    public byte[] string2bytes(String str) {
        if (null == str) {
            throw new NullPointerException("");
        }
        if (str.length() != 32) {
            throw new IllegalArgumentException("");
        }
        byte[] data = new byte[16];
        char[] chs = str.toCharArray();
        for (int i = 0; i < 16; ++i) {
            int h = rDigits.get(chs[i * 2]).intValue();
            int l = rDigits.get(chs[i * 2 + 1]).intValue();
            data[i] = (byte) ((h & 0x0F) << 4 | (l & 0x0F));
        }
        return data;
    }

}