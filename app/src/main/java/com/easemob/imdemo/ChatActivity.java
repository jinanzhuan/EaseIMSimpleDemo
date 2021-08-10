package com.easemob.imdemo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;

import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.easeui.EaseIM;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.provider.EaseUserProfileProvider;
import com.hyphenate.easeui.widget.EaseTitleBar;

public class ChatActivity extends BaseActivity{
    private EaseTitleBar titleBar;
    private String conversationId;
    private int chatType;
    private String historyMsgId;

    public static void actionStart(Context context, String conversationId, int chatType) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EaseConstant.EXTRA_CONVERSATION_ID, conversationId);
        intent.putExtra(EaseConstant.EXTRA_CHAT_TYPE, chatType);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        conversationId = getIntent().getStringExtra(EaseConstant.EXTRA_CONVERSATION_ID);
        chatType = getIntent().getIntExtra(EaseConstant.EXTRA_CHAT_TYPE, EaseConstant.CHATTYPE_SINGLE);
        historyMsgId = getIntent().getStringExtra(DemoConstant.HISTORY_MSG_ID);

        titleBar = findViewById(R.id.title_bar);
        titleBar.setOnBackPressListener(new EaseTitleBar.OnBackPressListener() {
            @Override
            public void onBackPress(View view) {
                onBackPressed();
            }
        });
        titleBar.setTitle("用户聊天");

        ChatFragment chatFragment = new ChatFragment();
        chatFragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fl_fragment, chatFragment)
                .commit();

        setDefaultTitle();
    }

    private void setDefaultTitle() {
        String title;
        if(chatType == DemoConstant.CHATTYPE_GROUP) {
            title = getGroupName(conversationId);
        }else if(chatType == DemoConstant.CHATTYPE_CHATROOM) {
            EMChatRoom room = EMClient.getInstance().chatroomManager().getChatRoom(conversationId);
            if(room == null) {
                getChatRoom(conversationId);
                return;
            }
            title =  TextUtils.isEmpty(room.getName()) ? conversationId : room.getName();
        }else {
            EaseUserProfileProvider userProvider = EaseIM.getInstance().getUserProvider();
            if(userProvider != null) {
                EaseUser user = userProvider.getUser(conversationId);
                if(user != null) {
                    title = user.getNickname();
                }else {
                    title = conversationId;
                }
            }else {
                title = conversationId;
            }
        }
        titleBar.setTitle(title);
    }

    private String getGroupName(String groupId) {
        EMGroup group = EMClient.getInstance().groupManager().getGroup(groupId);
        if(group == null) {
            return groupId;
        }
        return TextUtils.isEmpty(group.getGroupName()) ? groupId : group.getGroupName();
    }

    private void  getChatRoom(String roomId) {
        EMChatRoom room = EMClient.getInstance().chatroomManager().getChatRoom(roomId);
        if(room != null) {

        }else {
            EMClient.getInstance().chatroomManager().asyncFetchChatRoomFromServer(roomId, new EMValueCallBack<EMChatRoom>() {
                @Override
                public void onSuccess(EMChatRoom value) {
                    runOnUiThread(()->setDefaultTitle());
                }

                @Override
                public void onError(int error, String errorMsg) {
                    //
                }
            });
        }
    }
}
