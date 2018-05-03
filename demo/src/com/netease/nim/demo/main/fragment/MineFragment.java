package com.netease.nim.demo.main.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.netease.nim.demo.DemoCache;
import com.netease.nim.demo.R;
import com.netease.nim.demo.contact.activity.UserProfileSettingActivity;
import com.netease.nim.demo.main.activity.AboutActivity;
import com.netease.nim.demo.main.activity.SettingsActivity;
import com.netease.nim.demo.main.model.MainTab;
import com.netease.nim.uikit.business.uinfo.UserInfoHelper;
import com.netease.nim.uikit.common.ui.imageview.HeadImageView;

public class MineFragment extends MainTabFragment implements View.OnClickListener {

    private TextView mTvNickName;
    private TextView mTvAccount;
    private HeadImageView mHivAvatar;

    public MineFragment() {
        this.setContainerId(MainTab.MINE.fragmentId);
    }


    @Override
    protected void onInit() {
        findViews();
    }

    private void findViews() {
        findView(R.id.tv_mine_setting).setOnClickListener(this);
        findView(R.id.tv_mine_about).setOnClickListener(this);
        findView(R.id.rl_mine_personal_info).setOnClickListener(this);

        mTvNickName = findView(R.id.tv_mine_nick_name);
        mTvAccount = findView(R.id.tv_mine_account);
        mHivAvatar = findView(R.id.hiv_mine_avatar);


        handleData();

    }

    private void handleData() {
        mTvNickName.setText(UserInfoHelper.getUserDisplayName(DemoCache.getAccount()));
        mTvAccount.setText(String.format("帐号:%s", DemoCache.getAccount()));

        mHivAvatar.loadBuddyAvatar(DemoCache.getAccount());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        onCurrent(); // 触发onInit，提前加载
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_mine_setting: // 设置
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                break;
            case R.id.tv_mine_about: // 关于
                startActivity(new Intent(getActivity(), AboutActivity.class));
                break;
            case R.id.rl_mine_personal_info: // 个人信息
                UserProfileSettingActivity.start(getActivity(), DemoCache.getAccount());
                break;
        }
    }
}
