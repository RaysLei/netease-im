package com.abc.im.session.model;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;

import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.msg.model.TeamMsgAckInfo;

/**
 * Created on 2018/3/17.
 */

public class AckMsgViewModel extends ViewModel {
    private LiveData<TeamMsgAckInfo> teamMsgAckInfo;
    private AckModelRepository ackModelRepository;

    public void init(IMMessage message) {
        if (this.teamMsgAckInfo  != null) {
            return;
        }
        ackModelRepository = new AckModelRepository();
        teamMsgAckInfo = ackModelRepository.getMsgAckInfo(message);
    }

    public LiveData<TeamMsgAckInfo> getTeamMsgAckInfo() {
        return teamMsgAckInfo;
    }
}
