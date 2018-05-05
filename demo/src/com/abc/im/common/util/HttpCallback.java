package com.abc.im.common.util;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.abc.im.DemoCache;
import com.netease.nim.uikit.common.util.log.LogUtil;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public abstract class HttpCallback implements Callback {

    private static final String TAG = "HttpCallback";

    @Override
    public void onFailure(Call call, IOException e) {
        LogUtil.e(TAG, e.getMessage(), e);
        responseException("网络异常，请稍后再试。");
    }

    @Override
    public void onResponse(Call call, Response response) {
        int code = response.code();
        String url = response.request().url().toString();
        String body;
        try {
            body = response.body().string();
        } catch (IOException e) {
            LogUtil.e(TAG, e.getMessage(), e);
            responseException("数据异常。");
            return;
        }
        LogUtil.d(TAG, "url: " + url + " code: " + code + " body: " + body);
        Map<String, Object> resMap = JSON.parseObject(body, new TypeReference<Map<String, Object>>() {
        }.getType());
        int status = (int) resMap.get("status");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (1 == status) {
                    onResponseSuccess(resMap.get("data"));
                } else {
                    String message = (String) resMap.get("message");
                    onResponseFailure(status, message);
                }
                onResponseFinish();
            }
        });

    }

    private void responseException(String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                onResponseFailure(0, message);
                onResponseFinish();
            }
        });
    }

    public boolean isShowMessage() {
        return true;
    }

    public abstract void onResponseSuccess(Object resData);

    public void onResponseFailure(int status, String message) {
        if (isShowMessage() && !TextUtils.isEmpty(message)) {
            Toast.makeText(DemoCache.getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    public void onResponseFinish() {

    }

}
