package com.abc.im.config.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.abc.im.DemoCache;

import java.util.Collections;
import java.util.Map;

/**
 * Created by hzxuwen on 2015/4/13.
 */
public class Preferences {
    private static final String KEY_USER_ACCOUNT = "account";
    private static final String KEY_USER_TOKEN = "token";
    private static final String KEY_USER_INFO = "KEY_USER_INFO";

    public static void saveUserInfo(Map<String, Object> data) {
        saveString(KEY_USER_INFO, JSON.toJSONString(data));
    }

    public static Map<String, Object> getUserInfo() {
        String string = getString(KEY_USER_INFO);
        if (TextUtils.isEmpty(string)) {
            return Collections.emptyMap();
        }
        return JSON.parseObject(string, new TypeReference<Map<String, Object>>() {
        }.getType());
    }

    public static void saveUserAccount(String account) {
        saveString(KEY_USER_ACCOUNT, account);
    }

    public static String getUserAccount() {
        return getString(KEY_USER_ACCOUNT);
    }

    public static void saveUserToken(String token) {
        saveString(KEY_USER_TOKEN, token);
    }

    public static String getUserToken() {
        return getString(KEY_USER_TOKEN);
    }

    private static void saveString(String key, String value) {
        getSharedPreferences().edit().putString(key, value).apply();
    }

    private static String getString(String key) {
        return getSharedPreferences().getString(key, null);
    }

    private static SharedPreferences getSharedPreferences() {
        return DemoCache.getContext().getSharedPreferences("Demo", Context.MODE_PRIVATE);
    }
}
