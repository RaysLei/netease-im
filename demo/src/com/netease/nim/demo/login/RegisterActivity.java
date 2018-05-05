package com.netease.nim.demo.login;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.netease.nim.demo.R;
import com.netease.nim.demo.common.util.HttpCallback;
import com.netease.nim.demo.common.util.HttpUtils;
import com.netease.nim.uikit.api.wrapper.NimToolBarOptions;
import com.netease.nim.uikit.common.activity.ToolBarOptions;
import com.netease.nim.uikit.common.activity.UI;
import com.netease.nim.uikit.common.ui.dialog.DialogMaker;
import com.netease.nim.uikit.common.ui.widget.ClearableEditTextWithIcon;
import com.netease.nim.uikit.common.util.log.LogUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.netease.nim.demo.login.LoginActivity.MSG_CHANGE_CAPTCHA_TEXT;

public class RegisterActivity extends UI implements View.OnClickListener {

    private Button registerBtn;
    private TextView getCaptchaBtn;  // 获取验证码按钮

    private ClearableEditTextWithIcon phoneEdit;
    private ClearableEditTextWithIcon captchaEdit;
    private ClearableEditTextWithIcon passwordEdit;
    private ClearableEditTextWithIcon nicknameEdit;
    private ClearableEditTextWithIcon introducerdEdit;

    private MyHandler myHandler;
    private Map<String, Call> callMap = new HashMap<>();

    public static void start(Context context) {
        Intent intent = new Intent(context, RegisterActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.register_activity);

        ToolBarOptions options = new NimToolBarOptions();
        options.navigateId = R.drawable.nim_actionbar_white_back_icon;
        setToolBar(R.id.toolbar, options);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        init();
    }

    private void init() {
        myHandler = new MyHandler(this);

        getCaptchaBtn = findView(R.id.tv_get_captcha);
        getCaptchaBtn.setOnClickListener(this);

        registerBtn = findView(R.id.register_btn);
        registerBtn.setOnClickListener(this);

        phoneEdit = findView(R.id.edit_phone);
        phoneEdit.setIconResource(R.drawable.phone_icon);
        phoneEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11)});

        captchaEdit = findView(R.id.edit_captcha);
        captchaEdit.setIconResource(R.drawable.captcha_icon);
        captchaEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});

        passwordEdit = findView(R.id.edit_password);
        passwordEdit.setIconResource(R.drawable.user_pwd_lock_icon);
        passwordEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

        nicknameEdit = findView(R.id.edit_nickname);
        nicknameEdit.setIconResource(R.drawable.nickname_icon);
        nicknameEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});

        introducerdEdit = findView(R.id.edit_introducerd);
        introducerdEdit.setIconResource(R.drawable.introducerd_icon);
        introducerdEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_btn:
                onRegister();
                break;
            case R.id.tv_get_captcha:
                getCaptcha();
                break;
        }
    }

    private void getCaptcha() {
        String phone = phoneEdit.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            showToast("手机号不能为空");
            return;
        }
        final String requestName = "getCaptcha";

        DialogMaker.showProgressDialog(this, null, getString(R.string.get_captcha_ing), true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancelRequest(requestName);
            }
        }).setCanceledOnTouchOutside(false);

        cancelRequest(requestName);
        Call call = HttpUtils.getCaptcha(phone, new HttpCallback() {

            @Override
            public void onResponseSuccess(Object resData) {
                Message message = myHandler.obtainMessage(MSG_CHANGE_CAPTCHA_TEXT, 60, 1000);
                myHandler.sendMessage(message);
            }

            @Override
            public void onResponseFinish() {
                super.onResponseFinish();
                DialogMaker.dismissProgressDialog();
            }
        });
        callMap.put(requestName, call);
    }

    private void onRegister() {
        String phone = phoneEdit.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            showToast("手机号不能为空");
            return;
        }
        String captcha = captchaEdit.getText().toString().trim();
        if (TextUtils.isEmpty(captcha)) {
            showToast("验证码不能为空");
            return;
        } else if (captcha.length() != 4) {
            showToast("验证码无效");
            return;
        }
        String introducerd = introducerdEdit.getText().toString().trim();
        if (TextUtils.isEmpty(introducerd)) {
            showToast("邀请码不能为空");
            return;
        }
        String nickname = nicknameEdit.getText().toString().trim();
        if (TextUtils.isEmpty(nickname)) {
            showToast("昵称不能为空");
            return;
        }
        String password = passwordEdit.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            showToast("密码不能为空");
            return;
        } else if (password.length() < 6 || password.length() > 20) {
            showToast("密码无效");
            return;
        }

        final String requestName = "register";
        DialogMaker.showProgressDialog(this, null, getString(R.string.registering), true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancelRequest(requestName);
            }
        }).setCanceledOnTouchOutside(false);

        cancelRequest(requestName);
        Call call = HttpUtils.register(phone, captcha, password, nickname, introducerd, new HttpCallback() {

            @Override
            public void onResponseSuccess(Object resData) {
                showToast("注册成功");
                finish();
            }

            @Override
            public void onResponseFinish() {
                super.onResponseFinish();
                DialogMaker.dismissProgressDialog();
            }
        });
        callMap.put(requestName, call);
    }

    private static final class MyHandler extends Handler {

        private WeakReference<RegisterActivity> loginActivityWeakReference;

        MyHandler(RegisterActivity activity) {
            super(Looper.getMainLooper());
            loginActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CHANGE_CAPTCHA_TEXT:
                    RegisterActivity loginActivity = loginActivityWeakReference.get();
                    if (loginActivity == null || loginActivity.isDestroyedCompatible()) {
                        return;
                    }
                    if (msg.arg1 > 0) {
                        loginActivity.getCaptchaBtn.setClickable(false);
                        loginActivity.getCaptchaBtn.setText(loginActivity.getString(R.string.retry_get_captcha, msg.arg1 - 1));
                        Message message = this.obtainMessage(MSG_CHANGE_CAPTCHA_TEXT, msg.arg1 - 1, msg.arg2);
                        this.sendMessageDelayed(message, message.arg2);
                    } else {
                        loginActivity.getCaptchaBtn.setClickable(true);
                        loginActivity.getCaptchaBtn.setText(R.string.get_captcha);
                    }
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DialogMaker.dismissProgressDialog();
        cancelAllRequest();
        myHandler.removeCallbacksAndMessages(null);
    }

    private void cancelAllRequest() {
        for (Call call : callMap.values()) {
            if (!call.isCanceled()) {
                call.cancel();
            }
        }
        callMap.clear();
    }

    private void cancelRequest(String requestName) {
        Call call = callMap.get(requestName);
        if (call != null) {
            if (!call.isCanceled()) {
                call.cancel();
            }
            callMap.remove(requestName);
        }
    }
}
