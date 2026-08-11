package com.lyflexi.feignx.utils;

/**
 * 字符串工具类(自封装,不依赖 commons-lang3)
 *
 * @Author: feignx
 */
public final class StringUtil {

    private StringUtil() {
    }

    /**
     * 字符串为空、仅空白字符或长度为0时返回 true
     */
    public static boolean isBlank(String str) {
        if (str == null) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
}
