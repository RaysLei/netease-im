package com.abc.im.login;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.TextView;
import android.widget.Toast;

import com.abc.im.DemoCache;
import com.abc.im.R;
import com.abc.im.common.util.HttpCallback;
import com.abc.im.common.util.HttpUtils;
import com.abc.im.config.preference.Preferences;
import com.abc.im.config.preference.UserPreferences;
import com.abc.im.main.activity.MainActivity;
import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nim.uikit.api.wrapper.NimToolBarOptions;
import com.netease.nim.uikit.common.activity.ToolBarOptions;
import com.netease.nim.uikit.common.activity.UI;
import com.netease.nim.uikit.common.ui.dialog.DialogMaker;
import com.netease.nim.uikit.common.ui.dialog.EasyAlertDialogHelper;
import com.netease.nim.uikit.common.ui.widget.ClearableEditTextWithIcon;
import com.netease.nim.uikit.common.util.log.LogUtil;
import com.netease.nim.uikit.common.util.string.MD5;
import com.netease.nim.uikit.support.permission.MPermission;
import com.netease.nim.uikit.support.permission.annotation.OnMPermissionDenied;
import com.netease.nim.uikit.support.permission.annotation.OnMPermissionGranted;
import com.netease.nim.uikit.support.permission.annotation.OnMPermissionNeverAskAgain;
import com.netease.nimlib.sdk.AbortableFuture;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.RequestCallbackWrapper;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.StatusBarNotificationConfig;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.auth.ClientType;
import com.netease.nimlib.sdk.auth.LoginInfo;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 登录/注册界面
 * <p/>
 * Created by huangjun on 2015/2/1.
 */
public class LoginActivity extends UI implements OnKeyListener {

    public static final int MSG_CHANGE_CAPTCHA_TEXT = 1;

    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final String KICK_OUT = "KICK_OUT";
    private static final int BASIC_PERMISSION_REQUEST_CODE = 110;

    private TextView getCaptchaBtn;  // 获取验证码按钮
    private TextView loginTypeBtn;  // 登录类型按钮

    private ClearableEditTextWithIcon loginPhoneEdit;
    private ClearableEditTextWithIcon loginCaptchaEdit;
    private ClearableEditTextWithIcon loginPasswordEdit;

    private AbortableFuture<LoginInfo> loginRequest;

    private Map<String, Call> callMap = new HashMap<>();

    private MyHandler myHandler;

    public static void start(Context context) {
        start(context, false);
    }

    public static void start(Context context, boolean kickOut) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(KICK_OUT, kickOut);
        context.startActivity(intent);
    }

    @Override
    protected boolean displayHomeAsUpEnabled() {
        return false;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        ToolBarOptions options = new NimToolBarOptions();
        options.isNeedNavigate = false;
        options.titleId = 0;
        setToolBar(R.id.toolbar, options);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        requestBasicPermission();

        onParseIntent();
        initRightTopBtn();
        setupLoginPanel();

        myHandler = new MyHandler(this);
    }

    /**
     * 基本权限管理
     */

    private final String[] BASIC_PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private void requestBasicPermission() {
        MPermission.with(LoginActivity.this)
                .setRequestCode(BASIC_PERMISSION_REQUEST_CODE)
                .permissions(BASIC_PERMISSIONS)
                .request();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @OnMPermissionGranted(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionSuccess() {
        showToast("授权成功");
    }

    @OnMPermissionDenied(BASIC_PERMISSION_REQUEST_CODE)
    @OnMPermissionNeverAskAgain(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionFailed() {
        showToast("授权失败");
    }

    private void onParseIntent() {
        if (getIntent().getBooleanExtra(KICK_OUT, false)) {
            int type = NIMClient.getService(AuthService.class).getKickedClientType();
            String client;
            switch (type) {
                case ClientType.Web:
                    client = "网页端";
                    break;
                case ClientType.Windows:
                case ClientType.MAC:
                    client = "电脑端";
                    break;
                case ClientType.REST:
                    client = "服务端";
                    break;
                default:
                    client = "移动端";
                    break;
            }
            EasyAlertDialogHelper.showOneButtonDiolag(LoginActivity.this, getString(R.string.kickout_notify),
                    String.format(getString(R.string.kickout_content), client), getString(R.string.ok), true, null);
        }
    }

    /**
     * ActionBar 右上角按钮
     */
    private void initRightTopBtn() {
        findView(R.id.action_bar_right_clickable_textview).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                RegisterActivity.start(LoginActivity.this);
            }
        });
    }

    /**
     * 登录面板
     */
    private void setupLoginPanel() {
        loginTypeBtn = findView(R.id.tv_login_type);
        loginTypeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean isPasswordLogin = isPasswordLogin();
                isPasswordLogin = !isPasswordLogin;
                loginPhoneEdit.setText(null);
                if (isPasswordLogin) {
                    loginPhoneEdit.setHint(R.string.login_hint_account);
                    loginPhoneEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
                    loginPhoneEdit.setInputType(InputType.TYPE_CLASS_TEXT);
                    loginPasswordEdit.setVisibility(View.VISIBLE);
                    findView(R.id.ll_captcha_panel).setVisibility(View.GONE);
                    loginTypeBtn.setText(R.string.login_by_captcha);
                } else {
                    loginPhoneEdit.setHint(R.string.login_hint_phone);
                    loginPhoneEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11)});
                    loginPhoneEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
                    loginPasswordEdit.setVisibility(View.GONE);
                    findView(R.id.ll_captcha_panel).setVisibility(View.VISIBLE);
                    loginTypeBtn.setText(R.string.login_by_password);
                }
                v.setTag(isPasswordLogin);
            }
        });

        loginPhoneEdit = findView(R.id.edit_login_phone);
        loginPhoneEdit.setIconResource(R.drawable.phone_icon);
        loginPhoneEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11)});
//        String account = Preferences.getUserAccount();
//        loginPhoneEdit.setText(account);

        loginCaptchaEdit = findView(R.id.edit_login_captcha);
        loginCaptchaEdit.setIconResource(R.drawable.user_pwd_lock_icon);
        loginCaptchaEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        loginCaptchaEdit.setOnKeyListener(this);

        loginPasswordEdit = findView(R.id.edit_login_password);
        loginPasswordEdit.setIconResource(R.drawable.user_pwd_lock_icon);
        loginPasswordEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(31)});
        loginPasswordEdit.setOnKeyListener(this);

        getCaptchaBtn = findView(R.id.tv_get_captcha);
        getCaptchaBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getCaptcha(loginPhoneEdit.getText().toString());
            }
        });
    }

    @NonNull
    private Boolean isPasswordLogin() {
        Boolean isPasswordLogin = (Boolean) loginTypeBtn.getTag();
        if (isPasswordLogin == null) {
            isPasswordLogin = false;
        }
        return isPasswordLogin;
    }


    private void getCaptcha(String phone) {
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

    public void onLogin(View view) {
        LogUtil.d("onLogin", "is main thread: " + (Looper.getMainLooper() == Looper.myLooper()));
        if (isPasswordLogin()) {
            loginByPassword();
        } else {
            loginByCaptcha();
        }
    }

    private static final class MyHandler extends Handler {

        private WeakReference<LoginActivity> loginActivityWeakReference;

        MyHandler(LoginActivity loginActivity) {
            super(Looper.getMainLooper());
            loginActivityWeakReference = new WeakReference<>(loginActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CHANGE_CAPTCHA_TEXT:
                    LoginActivity loginActivity = loginActivityWeakReference.get();
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

    /**
     * 注册面板
     */
    /*private void setupRegisterPanel() {
        loginLayout = findView(R.id.login_layout);
        registerLayout = findView(R.id.register_layout);
        switchModeBtn = findView(R.id.register_login_tip);

        switchModeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode();
            }
        });
    }*/

    /**
     * ***************************************** 登录 **************************************
     */

    private void loginByPassword() {
        String account = loginPhoneEdit.getText().toString().trim();
        if (TextUtils.isEmpty(account)) {
            showToast("账号不能为空");
            return;
        }
        String password = loginPasswordEdit.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            showToast("密码不能为空");
            return;
        } else if (password.length() < 6 || password.length() > 20) {
            showToast("密码无效");
            return;
        }

        final String requestName = "loginByPassword";
        DialogMaker.showProgressDialog(this, null, getString(R.string.logining), true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancelRequest(requestName);
                if (loginRequest != null) {
                    loginRequest.abort();
                }
                onLoginDone();
            }
        }).setCanceledOnTouchOutside(false);

        Call call = HttpUtils.loginByPassword(account, password, new HttpCallback() {

            @Override
            public void onResponseSuccess(Object resData) {
                //noinspection unchecked
                Map<String, Object> data = (Map<String, Object>) resData;
                Preferences.saveUserInfo(data);
                String userId = (String) data.get("userId");
                String token = (String) data.get("token");
                imLogin(userId, token);
            }

            @Override
            public void onResponseFailure(int status, String message) {
                super.onResponseFailure(status, message);
                DialogMaker.dismissProgressDialog();
            }
        });
        callMap.put(requestName, call);
    }

    private void loginByCaptcha() {
        String phone = loginPhoneEdit.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            showToast("手机号不能为空");
            return;
        }
        String captcha = loginCaptchaEdit.getText().toString().trim();
        if (TextUtils.isEmpty(captcha)) {
            showToast("验证码不能为空");
            return;
        } else if (captcha.length() != 4) {
            showToast("验证码无效");
            return;
        }

        final String requestName = "loginByCaptcha";
        DialogMaker.showProgressDialog(this, null, getString(R.string.logining), true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancelRequest(requestName);
                if (loginRequest != null) {
                    loginRequest.abort();
                }
                onLoginDone();
            }
        }).setCanceledOnTouchOutside(false);

        Call call = HttpUtils.loginByCaptcha(phone, captcha, new HttpCallback() {

            @Override
            public void onResponseSuccess(Object resData) {
                    //noinspection unchecked
                    Map<String, Object> data = (Map<String, Object>) resData;
                    Preferences.saveUserInfo(data);
                    String userId = (String) data.get("userId");
                    String token = (String) data.get("token");
                    imLogin(userId, token);
            }

            @Override
            public void onResponseFailure(int status, String message) {
                super.onResponseFailure(status, message);
                DialogMaker.dismissProgressDialog();
            }
        });
        callMap.put(requestName, call);
    }

    private void imLogin(final String account, final String token) {
        // 云信只提供消息通道，并不包含用户资料逻辑。开发者需要在管理后台或通过服务器接口将用户帐号和token同步到云信服务器。
        // 在这里直接使用同步到云信服务器的帐号和token登录。
        // 这里为了简便起见，demo就直接使用了密码的md5作为token。
        // 如果开发者直接使用这个demo，只更改appkey，然后就登入自己的账户体系的话，需要传入同步到云信服务器的token，而不是用户密码。
        // 登录
        loginRequest = NimUIKit.login(new LoginInfo(account, token), new RequestCallback<LoginInfo>() {
            @Override
            public void onSuccess(LoginInfo param) {
                LogUtil.i(TAG, "login success");

                onLoginDone();

                DemoCache.setAccount(account);
                saveLoginInfo(account, token);

                // 初始化消息提醒配置
                initNotificationConfig();

                // 进入主界面
                MainActivity.start(LoginActivity.this, null);
                finish();
            }

            @Override
            public void onFailed(int code) {
                onLoginDone();
                if (code == 302 || code == 404) {
                    Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "登录失败: " + code, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onException(Throwable exception) {
                Toast.makeText(LoginActivity.this, R.string.login_exception, Toast.LENGTH_LONG).show();
                onLoginDone();
            }
        });
    }

    private void initNotificationConfig() {
        // 初始化消息提醒
        NIMClient.toggleNotification(UserPreferences.getNotificationToggle());

        // 加载状态栏配置
        StatusBarNotificationConfig statusBarNotificationConfig = UserPreferences.getStatusConfig();
        if (statusBarNotificationConfig == null) {
            statusBarNotificationConfig = DemoCache.getNotificationConfig();
            UserPreferences.setStatusConfig(statusBarNotificationConfig);
        }
        // 更新配置
        NIMClient.updateStatusBarNotificationConfig(statusBarNotificationConfig);
    }

    private void onLoginDone() {
        loginRequest = null;
        DialogMaker.dismissProgressDialog();
    }

    private void saveLoginInfo(final String account, final String token) {
        Preferences.saveUserAccount(account);
        Preferences.saveUserToken(token);
    }

    //DEMO中使用 username 作为 NIM 的account ，md5(password) 作为 token
    //开发者需要根据自己的实际情况配置自身用户系统和 NIM 用户系统的关系
    private String tokenFromPassword(String password) {
        String appKey = readAppKey(this);
        boolean isDemo = "45c6af3c98409b18a84451215d0bdd6e".equals(appKey)
                || "fe416640c8e8a72734219e1847ad2547".equals(appKey);

        return isDemo ? MD5.getStringMD5(password) : password;
    }

    private static String readAppKey(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo != null) {
                return appInfo.metaData.getString("com.netease.nim.appKey");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

//    private void register() {
//        if (!registerMode || !registerPanelInited) {
//            return;
//        }
//
//        if (!checkRegisterContentValid()) {
//            return;
//        }
//
//        if (!NetworkUtil.isNetAvailable(LoginActivity.this)) {
//            Toast.makeText(LoginActivity.this, R.string.network_is_not_available, Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        DialogMaker.showProgressDialog(this, getString(R.string.registering), false);
//
//        // 注册流程
//        final String account = registerAccountEdit.getText().toString();
//        final String nickName = registerNickNameEdit.getText().toString();
//        final String password = registerPasswordEdit.getText().toString();
//
//        ContactHttpClient.getInstance().register(account, nickName, password, new ContactHttpClient.ContactHttpCallback<Void>() {
//            @Override
//            public void onSuccess(Void aVoid) {
//                Toast.makeText(LoginActivity.this, R.string.register_success, Toast.LENGTH_SHORT).show();
//                switchMode();  // 切换回登录
//                loginPhoneEdit.setText(account);
//                loginCaptchaEdit.setText(password);
//
//                registerAccountEdit.setText("");
//                registerNickNameEdit.setText("");
//                registerPasswordEdit.setText("");
//
//                DialogMaker.dismissProgressDialog();
//            }
//
//            @Override
//            public void onFailed(int code, String errorMsg) {
//                Toast.makeText(LoginActivity.this, getString(R.string.register_failed, String.valueOf(code), errorMsg), Toast.LENGTH_SHORT)
//                        .show();
//
//                DialogMaker.dismissProgressDialog();
//            }
//        });
//    }

    /*private boolean checkRegisterContentValid() {
        if (!registerMode || !registerPanelInited) {
            return false;
        }

        // 帐号检查
        String account = registerAccountEdit.getText().toString().trim();
        if (account.length() <= 0 || account.length() > 20) {
            Toast.makeText(this, R.string.register_account_tip, Toast.LENGTH_SHORT).show();

            return false;
        }

        // 昵称检查
        String nick = registerNickNameEdit.getText().toString().trim();
        if (nick.length() <= 0 || nick.length() > 10) {
            Toast.makeText(this, R.string.register_nick_name_tip, Toast.LENGTH_SHORT).show();

            return false;
        }

        // 密码检查
        String password = registerPasswordEdit.getText().toString().trim();
        if (password.length() < 6 || password.length() > 20) {
            Toast.makeText(this, R.string.register_password_tip, Toast.LENGTH_SHORT).show();

            return false;
        }

        return true;
    }*/

    /*private void switchMode() {
        registerMode = !registerMode;

        if (registerMode && !registerPanelInited) {
            registerAccountEdit = findView(R.id.edit_register_account);
            registerNickNameEdit = findView(R.id.edit_register_nickname);
            registerPasswordEdit = findView(R.id.edit_register_password);

            registerAccountEdit.setIconResource(R.drawable.user_account_icon);
            registerNickNameEdit.setIconResource(R.drawable.nick_name_icon);
            registerPasswordEdit.setIconResource(R.drawable.user_pwd_lock_icon);

            registerAccountEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
            registerNickNameEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
            registerPasswordEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

            registerAccountEdit.addTextChangedListener(textWatcher);
            registerNickNameEdit.addTextChangedListener(textWatcher);
            registerPasswordEdit.addTextChangedListener(textWatcher);

            registerPanelInited = true;
        }

        setTitle(registerMode ? R.string.register : R.string.login);
        loginLayout.setVisibility(registerMode ? View.GONE : View.VISIBLE);
        registerLayout.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        switchModeBtn.setText(registerMode ? R.string.login_has_account : R.string.register);
        if (registerMode) {
            rightTopBtn.setEnabled(true);
        } else {
            boolean isEnable = loginPhoneEdit.getText().length() > 0
                    && loginCaptchaEdit.getText().length() > 0;
            rightTopBtn.setEnabled(isEnable);
        }
    }*/

//    public TextView addRegisterRightTopBtn(UI activity, int strResId) {
//        String text = activity.getResources().getString(strResId);
//        TextView textView = findView(R.id.action_bar_right_clickable_textview);
//        textView.setText(text);
//        if (textView != null) {
//            textView.setBackgroundResource(R.drawable.register_right_top_btn_selector);
//            textView.setPadding(ScreenUtil.dip2px(10), 0, ScreenUtil.dip2px(10), 0);
//        }
//        return textView;
//    }

    /**
     * *********** 假登录示例：假登录后，可以查看该用户数据，但向云信发送数据会失败；随后手动登录后可以发数据 **************
     */
    private void fakeLoginTest() {
        // 获取账号、密码；账号用于假登录，密码在手动登录时需要
        final String account = loginPhoneEdit.getEditableText().toString().toLowerCase();
        final String token = tokenFromPassword(loginCaptchaEdit.getEditableText().toString());

        // 执行假登录
        boolean res = NIMClient.getService(AuthService.class).openLocalCache(account); // SDK会将DB打开，支持查询。
        Log.i("test", "fake login " + (res ? "success" : "failed"));

        if (!res) {
            return;
        }

        // Demo缓存当前假登录的账号
        DemoCache.setAccount(account);

        // 初始化消息提醒配置
        initNotificationConfig();

        // 设置uikit
        NimUIKit.loginSuccess(account);

        // 进入主界面，此时可以查询数据（最近联系人列表、本地消息历史、群资料等都可以查询，但当云信服务器发起请求会返回408超时）
        MainActivity.start(LoginActivity.this, null);

        // 演示15s后手动登录，登录成功后，可以正常收发数据
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loginRequest = NIMClient.getService(AuthService.class).login(new LoginInfo(account, token));
                loginRequest.setCallback(new RequestCallbackWrapper() {
                    @Override
                    public void onResult(int code, Object result, Throwable exception) {
                        Log.i("test", "real login, code=" + code);
                        if (code == ResponseCode.RES_SUCCESS) {
                            saveLoginInfo(account, token);
                            finish();
                        }
                    }
                });
            }
        }, 15 * 1000);
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
