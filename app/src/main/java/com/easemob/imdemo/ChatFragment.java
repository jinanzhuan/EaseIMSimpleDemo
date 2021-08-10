package com.easemob.imdemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.modules.chat.EaseChatFragment;
import com.hyphenate.easeui.modules.chat.interfaces.IChatExtendMenu;
import com.hyphenate.easeui.modules.menu.EasePopupWindowHelper;
import com.hyphenate.util.EMFileHelper;
import com.hyphenate.util.EMLog;

public class ChatFragment extends EaseChatFragment {

    private static final String TAG = ChatFragment.class.getSimpleName();

    @Override
    public void initData() {
        super.initData();
        resetChatExtendMenu();

        LiveDataBus.get().with(DemoConstant.MESSAGE_CHANGE_CHANGE).postValue(new EaseEvent(DemoConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.TYPE.MESSAGE));

        LiveDataBus.get().with(DemoConstant.MESSAGE_CALL_SAVE, Boolean.class).observe(getViewLifecycleOwner(), event -> {
            if(event == null) {
                return;
            }
            if(event) {
                chatLayout.getChatMessageListLayout().refreshToLatest();
            }
        });

        LiveDataBus.get().with(DemoConstant.CONVERSATION_DELETE, EaseEvent.class).observe(getViewLifecycleOwner(), event -> {
            if(event == null) {
                return;
            }
            if(event.isMessageChange()) {
                chatLayout.getChatMessageListLayout().refreshMessages();
            }
        });

        LiveDataBus.get().with(DemoConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.class).observe(getViewLifecycleOwner(), event -> {
            if(event == null) {
                return;
            }
            if(event.isMessageChange()) {
                chatLayout.getChatMessageListLayout().refreshToLatest();
            }
        });
        LiveDataBus.get().with(DemoConstant.CONVERSATION_READ, EaseEvent.class).observe(getViewLifecycleOwner(), event -> {
            if(event == null) {
                return;
            }
            if(event.isMessageChange()) {
                chatLayout.getChatMessageListLayout().refreshMessages();
            }
        });

        //更新用户属性刷新列表
        LiveDataBus.get().with(DemoConstant.CONTACT_ADD, EaseEvent.class).observe(getViewLifecycleOwner(), event -> {
            if(event == null) {
                return;
            }
            if(event != null) {
                chatLayout.getChatMessageListLayout().refreshMessages();
            }
        });

        LiveDataBus.get().with(DemoConstant.CONTACT_UPDATE, EaseEvent.class).observe(getViewLifecycleOwner(), event -> {
            if(event == null) {
                return;
            }
            if(event != null) {
                chatLayout.getChatMessageListLayout().refreshMessages();
            }
        });
    }

    private void resetChatExtendMenu() {
        IChatExtendMenu chatExtendMenu = chatLayout.getChatInputMenu().getChatExtendMenu();
        chatExtendMenu.clear();
        chatExtendMenu.registerMenuItem(R.string.attach_picture, R.drawable.ease_chat_image_selector, R.id.extend_item_picture);
        chatExtendMenu.registerMenuItem(R.string.attach_take_pic, R.drawable.ease_chat_takepic_selector, R.id.extend_item_take_picture);
        chatExtendMenu.registerMenuItem(R.string.attach_video, R.drawable.em_chat_video_selector, R.id.extend_item_video);

        chatExtendMenu.registerMenuItem(R.string.attach_location, R.drawable.ease_chat_location_selector, R.id.extend_item_location);
        //chatExtendMenu.registerMenuItem(R.string.attach_file, R.drawable.em_chat_file_selector, R.id.extend_item_file);
    }

    @Override
    protected void selectVideoFromLocal() {
        super.selectVideoFromLocal();
//        Intent intent = new Intent(getActivity(), ImageGridActivity.class);
//        startActivityForResult(intent, REQUEST_CODE_SELECT_VIDEO);
    }

    @Override
    public void onPreMenu(EasePopupWindowHelper helper, EMMessage message) {
        //默认两分钟后，即不可撤回
        if(System.currentTimeMillis() - message.getMsgTime() > 2 * 60 * 1000) {
            helper.findItemVisible(R.id.action_chat_recall, false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SELECT_VIDEO: //send the video
                    if (data != null) {
                        int duration = data.getIntExtra("dur", 0);
                        String videoPath = data.getStringExtra("path");
                        String uriString = data.getStringExtra("uri");
                        EMLog.d(TAG, "path = "+videoPath + " uriString = "+uriString);
                        if(!TextUtils.isEmpty(videoPath)) {
                            chatLayout.sendVideoMessage(Uri.parse(videoPath), duration);
                        }else {
                            Uri videoUri = EMFileHelper.getInstance().formatInUri(uriString);
                            chatLayout.sendVideoMessage(videoUri, duration);
                        }
                    }
                    break;
            }
        }
    }
}
