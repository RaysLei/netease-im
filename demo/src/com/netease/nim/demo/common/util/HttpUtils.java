package com.netease.nim.demo.common.util;

import com.alibaba.fastjson.JSON;
import com.netease.nim.demo.config.AppConfig;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class HttpUtils {

    private static final OkHttpClient OK_HTTP_CLIENT;

    static {
        OK_HTTP_CLIENT = new OkHttpClient();
    }

    public static Call execute(Request request, Callback callback) {
        Call call = OK_HTTP_CLIENT.newCall(request);
        call.enqueue(callback);
        return call;
    }

    public static String getUrl(String url) {
        return AppConfig.SERVER_BASE_URL + url;
    }

    public static Call getCaptcha(String phone, Callback callback) {
        RequestBody body = new FormBody.Builder()
                .add("mobile", phone)
                .build();

        Request request = new Request.Builder()
                .url(getUrl("users/verification_code"))
                .post(body)
                .build();
        return execute(request, callback);
    }

    public static Call loginByCaptcha(String phone, String captcha, Callback callback) {
        RequestBody body = new FormBody.Builder()
                .add("appId", AppConfig.APP_ID)
                .add("mobile", phone)
                .add("verifycode", captcha)
                .build();
        Request request = new Request.Builder()
                .url(getUrl("users/verifycode_login"))
                .post(body)
                .build();
        return execute(request, callback);
    }

    public static Call loginByPassword(String account, String password, Callback callback) {
        RequestBody body = new FormBody.Builder()
                .add("appId", AppConfig.APP_ID)
                .add("account", account)
                .add("password", password)
                .build();
        Request request = new Request.Builder()
                .url(getUrl("users/password_login"))
                .post(body)
                .build();
        return execute(request, callback);
    }

    public static Call register(String phone, String captcha, String password, String nickname,
                                String introducerd, Callback callback) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("appId", AppConfig.APP_ID);
        paramMap.put("avatar", "");
        paramMap.put("introducer", introducerd);
        paramMap.put("mobile", phone);
        paramMap.put("name", nickname);
        paramMap.put("password", password);
        paramMap.put("verifycode", captcha);

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), JSON.toJSONString(paramMap));
        Request request = new Request.Builder()
                .url(getUrl("users/register"))
                .post(body)
                .build();
        return execute(request, callback);
    }
}
