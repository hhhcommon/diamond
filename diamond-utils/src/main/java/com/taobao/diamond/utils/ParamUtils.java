package com.taobao.diamond.utils;

/**
 * 
 * @author zh
 * 
 */
public class ParamUtils {

    private static char[] validChars = new char[] { '_', '-', '.', ':' };


    public static boolean isValid(String param) {
        if (param == null) {
            return false;
        }
        int length = param.length();
        for (int i = 0; i < length; i++) {
            char ch = param.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                continue;
            }
            else if (isValidChar(ch)) {
                continue;
            }
            else {
                return false;
            }
        }
        return true;
    }


    private static boolean isValidChar(char ch) {
        for (char c : validChars) {
            if (c == ch) {
                return true;
            }
        }
        return false;
    }

}
