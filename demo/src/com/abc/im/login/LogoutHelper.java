package com.abc.im.login;

import com.abc.im.DemoCache;
import com.abc.im.redpacket.NIMRedPacketClient;
import com.netease.nim.uikit.common.ui.drop.DropManager;
import com.netease.nim.uikit.api.NimUIKit;

/**
 * 注销帮助类
 * Created on 2015/10/8.
 */
public class LogoutHelper {
    public static void logout() {
        // 清理缓存&注销监听&清除状态
        NimUIKit.logout();
        DemoCache.clear();
        DropManager.getInstance().destroy();
        NIMRedPacketClient.clear();
    }
}
