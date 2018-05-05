package com.abc.im.main.dialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.abc.im.R;

public class ContactDialog extends DialogFragment implements View.OnClickListener {

    public static final int ADD_FRIENDS = 0x11;
    public static final int CREATE_GROUP = 0x22;
    public static final int SEARCH_GROUP = 0x33;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // 最好用RecyclerView自己封装个
        return inflater.inflate(R.layout.dialog_contact, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.tv_dialog_contact_add_friends).setOnClickListener(this);
        view.findViewById(R.id.tv_dialog_contact_create_group).setOnClickListener(this);
        view.findViewById(R.id.tv_dialog_contact_search_group).setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_dialog_contact_add_friends:
                if (onContactDialogItemClickListener != null) {
                    onContactDialogItemClickListener.onItemClick(ADD_FRIENDS);
                }
                break;
            case R.id.tv_dialog_contact_create_group:
                if (onContactDialogItemClickListener != null) {
                    onContactDialogItemClickListener.onItemClick(CREATE_GROUP);
                }
                break;
            case R.id.tv_dialog_contact_search_group:
                if (onContactDialogItemClickListener != null) {
                    onContactDialogItemClickListener.onItemClick(SEARCH_GROUP);
                }
                break;
        }
    }

    public interface OnContactDialogItemClickListener {
        void onItemClick(int type);
    }

    private OnContactDialogItemClickListener onContactDialogItemClickListener;

    public void setOnContactDialogItemClickListener(OnContactDialogItemClickListener onContactDialogItemClickListener) {
        this.onContactDialogItemClickListener = onContactDialogItemClickListener;
    }
}
