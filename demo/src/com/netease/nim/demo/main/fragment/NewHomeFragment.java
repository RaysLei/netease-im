package com.netease.nim.demo.main.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.netease.nim.demo.R;
import com.netease.nim.demo.common.ui.viewpager.FadeInOutPageTransformer;
import com.netease.nim.demo.main.adapter.MainTabPagerAdapter;
import com.netease.nim.demo.main.helper.SystemMessageUnreadManager;
import com.netease.nim.demo.main.model.MainTab;
import com.netease.nim.demo.main.reminder.ReminderItem;
import com.netease.nim.demo.main.reminder.ReminderManager;
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

/**
 * 云信主界面（导航页）
 */
public class NewHomeFragment extends TFragment implements ReminderManager.UnreadNumChangedCallback, TabLayout.OnTabSelectedListener {

    private ViewPager pager;

    private MainTabPagerAdapter adapter;

    private View rootView;
    private TabLayout mTabLayout;
    private TextView mTvTitle;

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
//        initUnreadCover();
    }

    /**
     * 查找页面控件
     */
    private void findViews() {
//        tabs = findView(R.id.tabs);
        mTabLayout = findView(R.id.new_main_tab_layout);
        pager = findView(R.id.new_main_tab_pager);
        mTvTitle = findView(R.id.tv_top_nav_title);
        mTvTitle.setText("会话");
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
            mTvNavTitle.setTextColor(0 == i ? getResources().getColor(R.color.grapefruitColor) : Color.parseColor("#333333"));
            mTvNavTitle.setText(mainTab.resId);

            mIvNavIcon.setImageResource(0 == i ? R.drawable.contact_selected : R.drawable.contact);
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

        ((TextView) view.findViewById(R.id.tv_nav_title)).setTextColor(getResources().getColor(R.color.grapefruitColor));
        ((ImageView) view.findViewById(R.id.iv_nav_icon)).setImageResource(R.drawable.contact_selected);

        mTvTitle.setText(MainTab.fromTabIndex(tab.getPosition()).resId);

    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

        View view = tab.getCustomView();
        ((TextView) view.findViewById(R.id.tv_nav_title)).setTextColor(Color.parseColor("#333333"));
        ((ImageView) view.findViewById(R.id.iv_nav_icon)).setImageResource(R.drawable.contact);
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }


}