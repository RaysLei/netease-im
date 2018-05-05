package com.abc.im.main.fragment;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.abc.im.R;
import com.abc.im.common.ui.viewpager.FadeInOutPageTransformer;
import com.abc.im.contact.activity.AddFriendActivity;
import com.abc.im.main.activity.MainActivity;
import com.abc.im.main.adapter.MainTabPagerAdapter;
import com.abc.im.main.dialog.ContactDialog;
import com.abc.im.main.helper.SystemMessageUnreadManager;
import com.abc.im.main.model.MainTab;
import com.abc.im.main.reminder.ReminderItem;
import com.abc.im.main.reminder.ReminderManager;
import com.abc.im.team.activity.AdvancedTeamSearchActivity;
import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nim.uikit.business.contact.selector.activity.ContactSelectActivity;
import com.netease.nim.uikit.business.team.helper.TeamHelper;
import com.netease.nim.uikit.common.fragment.TFragment;
import com.netease.nim.uikit.common.ui.drop.DropCover;
import com.netease.nim.uikit.common.ui.drop.DropManager;
import com.netease.nim.uikit.common.util.log.LogUtil;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.SystemMessageObserver;
import com.netease.nimlib.sdk.msg.SystemMessageService;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.RecentContact;
import com.netease.nimlib.sdk.util.NIMUtil;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 云信主界面（导航页）
 */
public class NewHomeFragment extends TFragment implements ReminderManager.UnreadNumChangedCallback, TabLayout.OnTabSelectedListener {

    private ViewPager pager;

    private MainTabPagerAdapter adapter;

    private View rootView;
    private TabLayout mTabLayout;
    private TextView mTvTitle;
    private ImageView mIvNavRight;

    public NewHomeFragment() {
        setContainerId(R.id.welcome_container);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.new_main, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        findViews();
        setupPager();
        setupTabs();

        registerMsgUnreadInfoObserver(true);
        registerSystemMessageObservers(true);
        requestSystemMessageUnreadCount();
        initUnreadCover();

        // test
//        OkHttpClient client = new OkHttpClient();//创建OkHttpClient对象。
//        FormBody.Builder formBody = new FormBody.Builder();//创建表单请求体
//        formBody.add("mobile ","16620095093");//传递键值对参数
//
//        Request request = new Request.Builder()//创建Request 对象。
//                .url("http://59.110.240.200/v1.0.0/users/verification_code")
//                .post(formBody.build())//传递请求体
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                e.printStackTrace();
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                String result = response.body().string();
//                System.out.println(result);
//            }
//        });

    }

    /**
     * 查找页面控件
     */
    private void findViews() {
//        tabs = findView(R.id.tabs);
        mTabLayout = findView(R.id.new_main_tab_layout);
        pager = findView(R.id.new_main_tab_pager);
        mTvTitle = findView(R.id.tv_top_nav_title);
        mIvNavRight = findView(R.id.iv_top_nav_right);
        mTvTitle.setText(getString(R.string.home_conversation));

        mIvNavRight.setOnClickListener((view) -> {

                    ContactDialog contactDialog = new ContactDialog();
                    contactDialog.show(getChildFragmentManager(), ContactDialog.class.getSimpleName());
                    contactDialog.setOnContactDialogItemClickListener((type)->{
                        contactDialog.dismiss();
                        switch (type){
                            case ContactDialog.ADD_FRIENDS: // 添加朋友
                                AddFriendActivity.start(getActivity());
                                break;
                            case ContactDialog.CREATE_GROUP: // 创建群聊
                                ContactSelectActivity.Option advancedOption = TeamHelper.getCreateContactSelectOption(null, 50);
                                NimUIKit.startContactSelector(getActivity(), advancedOption,2);
                                break;
                            case ContactDialog.SEARCH_GROUP: // 搜索群
                                AdvancedTeamSearchActivity.start(getActivity());
                                break;

                        }
                    });

                }
        );


    }


    @Override
    public void onResume() {
        super.onResume();
        enableMsgNotification(false);
        //quitOtherActivities();
    }

    @Override
    public void onPause() {
        super.onPause();
        enableMsgNotification(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        registerMsgUnreadInfoObserver(false);
        registerSystemMessageObservers(false);
    }

    public boolean onBackPressed() {
        return false;
    }

    public boolean onClick(View v) {
        return true;
    }

    /**
     * 设置viewPager
     */
    private void setupPager() {
        // CACHE COUNT
        adapter = new MainTabPagerAdapter(getFragmentManager(), getActivity(), pager);
        pager.setOffscreenPageLimit(adapter.getCacheCount());
        // page swtich animation
        pager.setPageTransformer(true, new FadeInOutPageTransformer());
        // ADAPTER
        pager.setAdapter(adapter);

    }

    // icon resIds
    private int[] imageResIds = {
            R.drawable.nav_msg,
            R.drawable.nav_contact,
            R.drawable.nav_mine
    };

    /**
     * 设置tab条目
     */
    private void setupTabs() {

        mTabLayout.setupWithViewPager(pager);

        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);

            View contentView = LayoutInflater.from(getActivity()).inflate(R.layout.item_nav, null, false);
            TextView mTvNavTitle = contentView.findViewById(R.id.tv_nav_title);
            ImageView mIvNavIcon = contentView.findViewById(R.id.iv_nav_icon);

            MainTab mainTab = MainTab.fromTabIndex(i);
            mTvNavTitle.setTextColor(0 == i ? getResources().getColor(R.color.navColor) : getResources().getColor(R.color.color_black_333333));
            mTvNavTitle.setText(mainTab.resId);

            mIvNavIcon.setImageResource(imageResIds[i]);
            if (0 == i) {
                changeImageViewColor(mIvNavIcon, R.color.navColor);
            }

            tab.setCustomView(contentView);

        }

        mTabLayout.addOnTabSelectedListener(this);

    }

    private void enableMsgNotification(boolean enable) {
        boolean msg = (pager.getCurrentItem() != MainTab.RECENT_CONTACTS.tabIndex);
        if (enable | msg) {
            /**
             * 设置最近联系人的消息为已读
             *
             * @param account,    聊天对象帐号，或者以下两个值：
             *                    {@link #MSG_CHATTING_ACCOUNT_ALL} 目前没有与任何人对话，但能看到消息提醒（比如在消息列表界面），不需要在状态栏做消息通知
             *                    {@link #MSG_CHATTING_ACCOUNT_NONE} 目前没有与任何人对话，需要状态栏消息通知
             */
            NIMClient.getService(MsgService.class).setChattingAccount(MsgService.MSG_CHATTING_ACCOUNT_NONE, SessionTypeEnum.None);
        } else {
            NIMClient.getService(MsgService.class).setChattingAccount(MsgService.MSG_CHATTING_ACCOUNT_ALL, SessionTypeEnum.None);
        }
    }

    /**
     * 注册未读消息数量观察者
     */
    private void registerMsgUnreadInfoObserver(boolean register) {
        if (register) {
            ReminderManager.getInstance().registerUnreadNumChangedCallback(this);
        } else {
            ReminderManager.getInstance().unregisterUnreadNumChangedCallback(this);
        }
    }

    /**
     * 未读消息数量观察者实现
     */
    @Override
    public void onUnreadNumChanged(ReminderItem item) {
        MainTab tab = MainTab.fromReminderId(item.getId());
        if (tab != null) {
//            tabs.updateTab(tab.tabIndex, item);
        }
    }

    /**
     * 注册/注销系统消息未读数变化
     *
     * @param register
     */
    private void registerSystemMessageObservers(boolean register) {
        NIMClient.getService(SystemMessageObserver.class).observeUnreadCountChange(sysMsgUnreadCountChangedObserver,
                register);
    }

    private Observer<Integer> sysMsgUnreadCountChangedObserver = new Observer<Integer>() {
        @Override
        public void onEvent(Integer unreadCount) {
            SystemMessageUnreadManager.getInstance().setSysMsgUnreadCount(unreadCount);
            ReminderManager.getInstance().updateContactUnreadNum(unreadCount);
        }
    };

    /**
     * 查询系统消息未读数
     */
    private void requestSystemMessageUnreadCount() {
        int unread = NIMClient.getService(SystemMessageService.class).querySystemMessageUnreadCountBlock();
        SystemMessageUnreadManager.getInstance().setSysMsgUnreadCount(unread);
        ReminderManager.getInstance().updateContactUnreadNum(unread);
    }

    /**
     * 初始化未读红点动画
     */
    private void initUnreadCover() {
        DropManager.getInstance().init(getContext(), (DropCover) findView(R.id.unread_cover),
                new DropCover.IDropCompletedListener() {
                    @Override
                    public void onCompleted(Object id, boolean explosive) {
                        if (id == null || !explosive) {
                            return;
                        }

                        if (id instanceof RecentContact) {
                            RecentContact r = (RecentContact) id;
                            NIMClient.getService(MsgService.class).clearUnreadCount(r.getContactId(), r.getSessionType());
                            LogUtil.i("HomeFragment", "clearUnreadCount, sessionId=" + r.getContactId());
                        } else if (id instanceof String) {
                            if (((String) id).contentEquals("0")) {
                                NIMClient.getService(MsgService.class).clearAllUnreadCount();
                                LogUtil.i("HomeFragment", "clearAllUnreadCount");
                            } else if (((String) id).contentEquals("1")) {
                                NIMClient.getService(SystemMessageService.class).resetSystemMessageUnreadCount();
                                LogUtil.i("HomeFragment", "clearAllSystemUnreadCount");
                            }
                        }
                    }
                });
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {

        View view = tab.getCustomView();

        ((TextView) view.findViewById(R.id.tv_nav_title)).setTextColor(getResources().getColor(R.color.navColor));
        changeImageViewColor(((ImageView) view.findViewById(R.id.iv_nav_icon)), R.color.navColor);

        mTvTitle.setText(MainTab.fromTabIndex(tab.getPosition()).resId);
        mIvNavRight.setVisibility(1 == tab.getPosition() ? View.VISIBLE : View.GONE);

    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

        View view = tab.getCustomView();
        ((TextView) view.findViewById(R.id.tv_nav_title)).setTextColor(getResources().getColor(R.color.color_black_333333));
        ((ImageView) view.findViewById(R.id.iv_nav_icon)).setImageResource(imageResIds[tab.getPosition()]);
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    /**
     * 更改ImageView的颜色
     *
     * @param view       ImageView
     * @param colorResId colorId
     */
    public static void changeImageViewColor(ImageView view, int colorResId) {
        //mutate()
        Drawable modeDrawable = view.getDrawable().mutate();
        Drawable temp = DrawableCompat.wrap(modeDrawable);
        ColorStateList colorStateList = ColorStateList.valueOf(view.getResources().getColor(colorResId));
        DrawableCompat.setTintList(temp, colorStateList);
        view.setImageDrawable(temp);
    }


}