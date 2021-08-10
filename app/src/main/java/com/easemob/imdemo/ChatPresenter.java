package com.easemob.imdemo;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.hyphenate.EMChatRoomChangeListener;
import com.hyphenate.EMConnectionListener;
import com.hyphenate.EMContactListener;
import com.hyphenate.EMConversationListener;
import com.hyphenate.EMError;
import com.hyphenate.EMMultiDeviceListener;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMMucSharedFile;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.chat.EMUserInfo;
import com.hyphenate.easeui.interfaces.EaseGroupListener;
import com.hyphenate.easeui.manager.EaseAtMessageHelper;
import com.hyphenate.easeui.manager.EaseChatPresenter;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.util.EMLog;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 主要用于chat过程中的全局监听，并对相应的事件进行处理
 * {@link #init()}方法建议在登录成功以后进行调用
 */
public class ChatPresenter extends EaseChatPresenter {
    private static final String TAG = ChatPresenter.class.getSimpleName();
    private static final int HANDLER_SHOW_TOAST = 0;
    private static ChatPresenter instance;
    private LiveDataBus messageChangeLiveData;
    private boolean isGroupsSyncedWithServer = false;
    private boolean isContactsSyncedWithServer = false;
    private boolean isBlackListSyncedWithServer = false;
    private boolean isPushConfigsWithServer = false;
    private Context appContext;
    protected Handler handler;

    Queue<String> msgQueue = new ConcurrentLinkedQueue<>();

    private ChatPresenter() {
        appContext = DemoApplication.getInstance();
        initHandler(appContext.getMainLooper());
        messageChangeLiveData = LiveDataBus.get();
        //添加网络连接状态监听
        DemoHelper.getInstance().getEMClient().addConnectionListener(new ChatConnectionListener());
        //添加多端登录监听
        DemoHelper.getInstance().getEMClient().addMultiDeviceListener(new ChatMultiDeviceListener());
        //添加群组监听
        DemoHelper.getInstance().getGroupManager().addGroupChangeListener(new ChatGroupListener());
        //添加联系人监听
        DemoHelper.getInstance().getContactManager().setContactListener(new ChatContactListener());
        //添加聊天室监听
        DemoHelper.getInstance().getChatroomManager().addChatRoomChangeListener(new ChatRoomListener());
        //添加对会话的监听（监听已读回执）
        DemoHelper.getInstance().getChatManager().addConversationListener(new ChatConversationListener());
    }

    public static ChatPresenter getInstance() {
        if(instance == null) {
            synchronized (ChatPresenter.class) {
                if(instance == null) {
                    instance = new ChatPresenter();
                }
            }
        }
        return instance;
    }

    /**
     * 将需要登录成功进入MainActivity中初始化的逻辑，放到此处进行处理
     */
    public void init() {

    }

    public void initHandler(Looper looper) {
        handler = new Handler(looper) {
            @Override
            public void handleMessage(Message msg) {
                Object obj = msg.obj;
                switch (msg.what) {
                    case HANDLER_SHOW_TOAST :
                        if(obj instanceof String) {
                            String str = (String) obj;
                            //ToastUtils.showToast(str);
                            Toast.makeText(appContext, str, Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        };
        while (!msgQueue.isEmpty()) {
            showToast(msgQueue.remove());
        }
    }

    void showToast(@StringRes int mesId) {
        showToast(context.getString(mesId));
    }

    void showToast(final String message) {
        Log.d(TAG, "receive invitation to join the group：" + message);
        if (handler != null) {
            Message msg = Message.obtain(handler, HANDLER_SHOW_TOAST, message);
            handler.sendMessage(msg);
        } else {
            msgQueue.add(message);
        }
    }

    @Override
    public void onMessageReceived(List<EMMessage> messages) {
        super.onMessageReceived(messages);
        EaseEvent event = EaseEvent.create(DemoConstant.MESSAGE_CHANGE_RECEIVE, EaseEvent.TYPE.MESSAGE);
        messageChangeLiveData.with(DemoConstant.MESSAGE_CHANGE_CHANGE).postValue(event);
        for (EMMessage message : messages) {
            EMLog.d(TAG, "onMessageReceived id : " + message.getMsgId());
            EMLog.d(TAG, "onMessageReceived: " + message.getType());
            // 如果设置群组离线消息免打扰，则不进行消息通知
            List<String> disabledIds = DemoHelper.getInstance().getPushManager().getNoPushGroups();
            if(disabledIds != null && disabledIds.contains(message.conversationId())) {
                return;
            }
            // in background, do not refresh UI, notify it in notification bar
            if(!DemoApplication.getInstance().getLifecycleCallbacks().isFront()){
                getNotifier().notify(message);
            }
            //notify new message
            getNotifier().vibrateAndPlayTone(message);
        }
    }



    /**
     * 判断是否已经启动了MainActivity
     * @return
     */
    private synchronized boolean isAppLaunchMain() {
        List<Activity> activities = DemoApplication.getInstance().getLifecycleCallbacks().getActivityList();
        if(activities != null && !activities.isEmpty()) {
            for(int i = activities.size() - 1; i >= 0 ; i--) {
                if(activities.get(i) instanceof MainActivity) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onCmdMessageReceived(List<EMMessage> messages) {
        super.onCmdMessageReceived(messages);
        EaseEvent event = EaseEvent.create(DemoConstant.MESSAGE_CHANGE_CMD_RECEIVE, EaseEvent.TYPE.MESSAGE);
        messageChangeLiveData.with(DemoConstant.MESSAGE_CHANGE_CHANGE).postValue(event);
    }

    @Override
    public void onMessageRead(List<EMMessage> messages) {
        super.onMessageRead(messages);
        if(!(DemoApplication.getInstance().getLifecycleCallbacks().current() instanceof ChatActivity)) {
            EaseEvent event = EaseEvent.create(DemoConstant.MESSAGE_CHANGE_RECALL, EaseEvent.TYPE.MESSAGE);
            messageChangeLiveData.with(DemoConstant.MESSAGE_CHANGE_CHANGE).postValue(event);
        }
    }

    @Override
    public void onMessageRecalled(List<EMMessage> messages) {
        EaseEvent event = EaseEvent.create(DemoConstant.MESSAGE_CHANGE_RECALL, EaseEvent.TYPE.MESSAGE);
        messageChangeLiveData.with(DemoConstant.MESSAGE_CHANGE_CHANGE).postValue(event);
        for (EMMessage msg : messages) {
            if(msg.getChatType() == EMMessage.ChatType.GroupChat && EaseAtMessageHelper.get().isAtMeMsg(msg)){
                EaseAtMessageHelper.get().removeAtMeGroup(msg.getTo());
            }
            EMMessage msgNotification = EMMessage.createReceiveMessage(EMMessage.Type.TXT);
            EMTextMessageBody txtBody = new EMTextMessageBody(String.format(context.getString(R.string.msg_recall_by_user), msg.getFrom()));
            msgNotification.addBody(txtBody);
            msgNotification.setFrom(msg.getFrom());
            msgNotification.setTo(msg.getTo());
            msgNotification.setUnread(false);
            msgNotification.setMsgTime(msg.getMsgTime());
            msgNotification.setLocalTime(msg.getMsgTime());
            msgNotification.setChatType(msg.getChatType());
            msgNotification.setAttribute(DemoConstant.MESSAGE_TYPE_RECALL, true);
            msgNotification.setStatus(EMMessage.Status.SUCCESS);
            EMClient.getInstance().chatManager().saveMessage(msgNotification);
        }
    }

    private class ChatConversationListener implements EMConversationListener {

        @Override
        public void onCoversationUpdate() {

        }

        @Override
        public void onConversationRead(String from, String to) {
            EaseEvent event = EaseEvent.create(DemoConstant.CONVERSATION_READ, EaseEvent.TYPE.MESSAGE);
            messageChangeLiveData.with(DemoConstant.CONVERSATION_READ).postValue(event);
        }
    }

    private class ChatConnectionListener implements EMConnectionListener {

        @Override
        public void onConnected() {
            EMLog.i(TAG, "onConnected");
        }

        /**
         * 用来监听账号异常
         * @param error
         */
        @Override
        public void onDisconnected(int error) {
            EMLog.i(TAG, "onDisconnected ="+error);
            String event = null;
            if (error == EMError.USER_REMOVED) {
                event = DemoConstant.ACCOUNT_REMOVED;
            } else if (error == EMError.USER_LOGIN_ANOTHER_DEVICE) {
                event = DemoConstant.ACCOUNT_CONFLICT;
            } else if (error == EMError.SERVER_SERVICE_RESTRICTED) {
                event = DemoConstant.ACCOUNT_FORBIDDEN;
            } else if (error == EMError.USER_KICKED_BY_CHANGE_PASSWORD) {
                event = DemoConstant.ACCOUNT_KICKED_BY_CHANGE_PASSWORD;
            } else if (error == EMError.USER_KICKED_BY_OTHER_DEVICE) {
                event = DemoConstant.ACCOUNT_KICKED_BY_OTHER_DEVICE;
            }
            if(!TextUtils.isEmpty(event)) {
                LiveDataBus.get().with(DemoConstant.ACCOUNT_CHANGE).postValue(new EaseEvent(event, EaseEvent.TYPE.ACCOUNT));
                EMLog.i(TAG, event);
            }
        }
    }

    private class ChatGroupListener extends EaseGroupListener {

        @Override
        public void onInvitationReceived(String groupId, String groupName, String inviter, String reason) {
            super.onInvitationReceived(groupId, groupName, inviter, reason);
            EaseEvent event = EaseEvent.create(DemoConstant.NOTIFY_GROUP_INVITE_RECEIVE, EaseEvent.TYPE.NOTIFY);
            messageChangeLiveData.with(DemoConstant.NOTIFY_CHANGE).postValue(event);
        }

        @Override
        public void onInvitationAccepted(String groupId, String invitee, String reason) {
            super.onInvitationAccepted(groupId, invitee, reason);
            //user accept your invitation
            EaseEvent event = EaseEvent.create(DemoConstant.NOTIFY_GROUP_INVITE_ACCEPTED, EaseEvent.TYPE.NOTIFY);
            messageChangeLiveData.with(DemoConstant.NOTIFY_CHANGE).postValue(event);
        }

        @Override
        public void onInvitationDeclined(String groupId, String invitee, String reason) {
            super.onInvitationDeclined(groupId, invitee, reason);
            EaseEvent event = EaseEvent.create(DemoConstant.NOTIFY_GROUP_INVITE_DECLINED, EaseEvent.TYPE.NOTIFY);
            messageChangeLiveData.with(DemoConstant.NOTIFY_CHANGE).postValue(event);
        }

        @Override
        public void onUserRemoved(String groupId, String groupName) {
            EaseEvent easeEvent = new EaseEvent(DemoConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP_LEAVE);
            easeEvent.message = groupId;
            messageChangeLiveData.with(DemoConstant.GROUP_CHANGE).postValue(easeEvent);
        }

        @Override
        public void onGroupDestroyed(String groupId, String groupName) {
            EaseEvent easeEvent = new EaseEvent(DemoConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP_LEAVE);
            easeEvent.message = groupId;
            messageChangeLiveData.with(DemoConstant.GROUP_CHANGE).postValue(easeEvent);
        }

        @Override
        public void onRequestToJoinReceived(String groupId, String groupName, String applicant, String reason) {
            super.onRequestToJoinReceived(groupId, groupName, applicant, reason);
            EaseEvent event = EaseEvent.create(DemoConstant.NOTIFY_GROUP_JOIN_RECEIVE, EaseEvent.TYPE.NOTIFY);
            messageChangeLiveData.with(DemoConstant.NOTIFY_CHANGE).postValue(event);
        }

        @Override
        public void onRequestToJoinAccepted(String groupId, String groupName, String accepter) {
            super.onRequestToJoinAccepted(groupId, groupName, accepter);
            EaseEvent event = EaseEvent.create(DemoConstant.MESSAGE_GROUP_JOIN_ACCEPTED, EaseEvent.TYPE.MESSAGE);
            messageChangeLiveData.with(DemoConstant.MESSAGE_CHANGE_CHANGE).postValue(event);
        }

        @Override
        public void onRequestToJoinDeclined(String groupId, String groupName, String decliner, String reason) {
            super.onRequestToJoinDeclined(groupId, groupName, decliner, reason);
        }

        @Override
        public void onAutoAcceptInvitationFromGroup(String groupId, String inviter, String inviteMessage) {
            super.onAutoAcceptInvitationFromGroup(groupId, inviter, inviteMessage);
            EaseEvent event = EaseEvent.create(DemoConstant.MESSAGE_GROUP_AUTO_ACCEPT, EaseEvent.TYPE.MESSAGE);
            messageChangeLiveData.with(DemoConstant.MESSAGE_CHANGE_CHANGE).postValue(event);
        }

        @Override
        public void onMuteListAdded(String groupId, List<String> mutes, long muteExpire) {
            super.onMuteListAdded(groupId, mutes, muteExpire);
        }

        @Override
        public void onMuteListRemoved(String groupId, List<String> mutes) {
            super.onMuteListRemoved(groupId, mutes);
        }

        @Override
        public void onWhiteListAdded(String groupId, List<String> whitelist) {
            EaseEvent easeEvent = new EaseEvent(DemoConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP);
            easeEvent.message = groupId;
            messageChangeLiveData.with(DemoConstant.GROUP_CHANGE).postValue(easeEvent);
        }

        @Override
        public void onWhiteListRemoved(String groupId, List<String> whitelist) {
            EaseEvent easeEvent = new EaseEvent(DemoConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP);
            easeEvent.message = groupId;
            messageChangeLiveData.with(DemoConstant.GROUP_CHANGE).postValue(easeEvent);
        }

        @Override
        public void onAllMemberMuteStateChanged(String groupId, boolean isMuted) {
            EaseEvent easeEvent = new EaseEvent(DemoConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP);
            easeEvent.message = groupId;
            messageChangeLiveData.with(DemoConstant.GROUP_CHANGE).postValue(easeEvent);
        }

        @Override
        public void onAdminAdded(String groupId, String administrator) {
            super.onAdminAdded(groupId, administrator);
            LiveDataBus.get().with(DemoConstant.GROUP_CHANGE).postValue(EaseEvent.create(DemoConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP));
        }

        @Override
        public void onAdminRemoved(String groupId, String administrator) {
            LiveDataBus.get().with(DemoConstant.GROUP_CHANGE).postValue(EaseEvent.create(DemoConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP));
        }

        @Override
        public void onOwnerChanged(String groupId, String newOwner, String oldOwner) {
            LiveDataBus.get().with(DemoConstant.GROUP_CHANGE).postValue(EaseEvent.create(DemoConstant.GROUP_OWNER_TRANSFER, EaseEvent.TYPE.GROUP));
        }

        @Override
        public void onMemberJoined(String groupId, String member) {
            LiveDataBus.get().with(DemoConstant.GROUP_CHANGE).postValue(EaseEvent.create(DemoConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP));
        }

        @Override
        public void onMemberExited(String groupId, String member) {
            LiveDataBus.get().with(DemoConstant.GROUP_CHANGE).postValue(EaseEvent.create(DemoConstant.GROUP_CHANGE, EaseEvent.TYPE.GROUP));
        }

        @Override
        public void onAnnouncementChanged(String groupId, String announcement) {
        }

        @Override
        public void onSharedFileAdded(String groupId, EMMucSharedFile sharedFile) {
            LiveDataBus.get().with(DemoConstant.GROUP_SHARE_FILE_CHANGE).postValue(EaseEvent.create(DemoConstant.GROUP_SHARE_FILE_CHANGE, EaseEvent.TYPE.GROUP));
        }

        @Override
        public void onSharedFileDeleted(String groupId, String fileId) {
            LiveDataBus.get().with(DemoConstant.GROUP_SHARE_FILE_CHANGE).postValue(EaseEvent.create(DemoConstant.GROUP_SHARE_FILE_CHANGE, EaseEvent.TYPE.GROUP));
        }
    }

    private class ChatContactListener implements EMContactListener {

        @Override
        public void onContactAdded(String username) {
            EMLog.i("ChatContactListener", "onContactAdded");
            String[] userId = new String[1];
            userId[0] = username;
            EMClient.getInstance().userInfoManager().fetchUserInfoByUserId(userId, new EMValueCallBack<Map<String, EMUserInfo>>() {
                @Override
                public void onSuccess(Map<String, EMUserInfo> value) {
                    EaseEvent event = EaseEvent.create(DemoConstant.CONTACT_ADD, EaseEvent.TYPE.CONTACT);
                    event.message = username;
                    messageChangeLiveData.with(DemoConstant.CONTACT_ADD).postValue(event);
                }

                @Override
                public void onError(int error, String errorMsg) {
                    EMLog.i(TAG, username + "error:" + error + " errorMsg:" +errorMsg);
                }
            });
        }

        @Override
        public void onContactDeleted(String username) {
            EMLog.i("ChatContactListener", "onContactDeleted");
            EaseEvent event = EaseEvent.create(DemoConstant.CONTACT_DELETE, EaseEvent.TYPE.CONTACT);
            event.message = username;
            messageChangeLiveData.with(DemoConstant.CONTACT_DELETE).postValue(event);
        }



        @Override
        public void onContactInvited(String username, String reason) {
            EMLog.i("ChatContactListener", "onContactInvited");
            EaseEvent event = EaseEvent.create(DemoConstant.CONTACT_CHANGE, EaseEvent.TYPE.CONTACT);
            messageChangeLiveData.with(DemoConstant.CONTACT_CHANGE).postValue(event);
        }

        @Override
        public void onFriendRequestAccepted(String username) {
            EMLog.i("ChatContactListener", "onFriendRequestAccepted");
            EaseEvent event = EaseEvent.create(DemoConstant.CONTACT_CHANGE, EaseEvent.TYPE.CONTACT);
            messageChangeLiveData.with(DemoConstant.CONTACT_CHANGE).postValue(event);
        }

        @Override
        public void onFriendRequestDeclined(String username) {
            EMLog.i("ChatContactListener", "onFriendRequestDeclined");
            EaseEvent event = EaseEvent.create(DemoConstant.CONTACT_CHANGE, EaseEvent.TYPE.CONTACT);
            messageChangeLiveData.with(DemoConstant.CONTACT_CHANGE).postValue(event);
        }
    }

    private class ChatMultiDeviceListener implements EMMultiDeviceListener {


        @Override
        public void onContactEvent(int event, String target, String ext) {
            EMLog.i(TAG, "onContactEvent event"+event);
            String message = null;
            switch (event) {
                case CONTACT_REMOVE: //好友已经在其他机子上被移除
                    EMLog.i("ChatMultiDeviceListener", "CONTACT_REMOVE");
                    message = DemoConstant.CONTACT_REMOVE;
                    // TODO: 2020/1/16 0016 确认此处逻辑，是否是删除当前的target
                    DemoHelper.getInstance().getChatManager().deleteConversation(target, false);

                    showToast("CONTACT_REMOVE");
                    break;
                case CONTACT_ACCEPT: //好友请求已经在其他机子上被同意
                    EMLog.i("ChatMultiDeviceListener", "CONTACT_ACCEPT");
                    message = DemoConstant.CONTACT_ACCEPT;

                    showToast("CONTACT_ACCEPT");
                    break;
                case CONTACT_DECLINE: //好友请求已经在其他机子上被拒绝
                    EMLog.i("ChatMultiDeviceListener", "CONTACT_DECLINE");
                    message = DemoConstant.CONTACT_DECLINE;

                    showToast("CONTACT_DECLINE");
                    break;
                case CONTACT_BAN: //当前用户在其他设备加某人进入黑名单
                    EMLog.i("ChatMultiDeviceListener", "CONTACT_BAN");
                    message = DemoConstant.CONTACT_BAN;
                    DemoHelper.getInstance().getChatManager().deleteConversation(target, false);

                    showToast("CONTACT_BAN");
                    break;
                case CONTACT_ALLOW: // 好友在其他设备被移出黑名单
                    EMLog.i("ChatMultiDeviceListener", "CONTACT_ALLOW");
                    message = DemoConstant.CONTACT_ALLOW;

                    showToast("CONTACT_ALLOW");
                    break;
            }
            if(!TextUtils.isEmpty(message)) {
                EaseEvent easeEvent = EaseEvent.create(message, EaseEvent.TYPE.CONTACT);
                messageChangeLiveData.with(message).postValue(easeEvent);
            }
        }

        @Override
        public void onGroupEvent(int event, String groupId, List<String> usernames) {
            EMLog.i(TAG, "onGroupEvent event"+event);
            String message = null;
            switch (event) {
                case GROUP_CREATE:

                    showToast("GROUP_CREATE");
                    break;
                case GROUP_DESTROY:
                    message = DemoConstant.GROUP_CHANGE;

                    showToast("GROUP_DESTROY");
                    break;
                case GROUP_JOIN:
                    message = DemoConstant.GROUP_CHANGE;

                    showToast("GROUP_JOIN");
                    break;
                case GROUP_LEAVE:
                    message = DemoConstant.GROUP_CHANGE;

                    showToast("GROUP_LEAVE");
                    break;
                case GROUP_APPLY:
                    showToast("GROUP_APPLY");
                    break;
                case GROUP_APPLY_ACCEPT:
                    showToast("GROUP_APPLY_ACCEPT");
                    break;
                case GROUP_APPLY_DECLINE:
                    showToast("GROUP_APPLY_DECLINE");
                    break;
                case GROUP_INVITE:
                    showToast("GROUP_INVITE");
                    break;
                case GROUP_INVITE_ACCEPT:
                    message = DemoConstant.GROUP_CHANGE;

                    showToast("GROUP_INVITE_ACCEPT");
                    break;
                case GROUP_INVITE_DECLINE:
                    showToast("GROUP_INVITE_DECLINE");
                    break;
                case GROUP_KICK:
                    message = DemoConstant.GROUP_CHANGE;

                    showToast("GROUP_KICK");
                    break;
                case GROUP_BAN:
                    message = DemoConstant.GROUP_CHANGE;

                    showToast("GROUP_BAN");
                    break;
                case GROUP_ALLOW:
                    showToast("GROUP_ALLOW");
                    break;
                case GROUP_BLOCK:
                    showToast("GROUP_BLOCK");
                    break;
                case GROUP_UNBLOCK:
                    showToast("GROUP_UNBLOCK");
                    break;
                case GROUP_ASSIGN_OWNER:
                    showToast("GROUP_ASSIGN_OWNER");
                    break;
                case GROUP_ADD_ADMIN:
                    message = DemoConstant.GROUP_CHANGE;

                    showToast("GROUP_ADD_ADMIN");
                    break;
                case GROUP_REMOVE_ADMIN:
                    message = DemoConstant.GROUP_CHANGE;

                    showToast("GROUP_REMOVE_ADMIN");
                    break;
                case GROUP_ADD_MUTE:
                    showToast("GROUP_ADD_MUTE");
                    break;
                case GROUP_REMOVE_MUTE:
                    showToast("GROUP_REMOVE_MUTE");
                    break;
                default:
                    break;
            }
            if(!TextUtils.isEmpty(message)) {
                EaseEvent easeEvent = EaseEvent.create(message, EaseEvent.TYPE.GROUP);
                messageChangeLiveData.with(message).postValue(easeEvent);
            }
        }
    }


    private class ChatRoomListener implements EMChatRoomChangeListener {

        @Override
        public void onChatRoomDestroyed(String roomId, String roomName) {
            setChatRoomEvent(roomId, EaseEvent.TYPE.CHAT_ROOM_LEAVE);
        }

        @Override
        public void onMemberJoined(String roomId, String participant) {
            setChatRoomEvent(roomId, EaseEvent.TYPE.CHAT_ROOM);
        }

        @Override
        public void onMemberExited(String roomId, String roomName, String participant) {
            setChatRoomEvent(roomId, EaseEvent.TYPE.CHAT_ROOM);
        }

        @Override
        public void onRemovedFromChatRoom(int reason, String roomId, String roomName, String participant) {
            if(TextUtils.equals(DemoHelper.getInstance().getCurrentUser(), participant)) {
                setChatRoomEvent(roomId, EaseEvent.TYPE.CHAT_ROOM);
            }
        }

        @Override
        public void onMuteListAdded(String chatRoomId, List<String> mutes, long expireTime) {
            setChatRoomEvent(chatRoomId, EaseEvent.TYPE.CHAT_ROOM);
        }

        @Override
        public void onMuteListRemoved(String chatRoomId, List<String> mutes) {
            setChatRoomEvent(chatRoomId, EaseEvent.TYPE.CHAT_ROOM);
        }

        @Override
        public void onWhiteListAdded(String chatRoomId, List<String> whitelist) {
        }

        @Override
        public void onWhiteListRemoved(String chatRoomId, List<String> whitelist) {
        }

        @Override
        public void onAllMemberMuteStateChanged(String chatRoomId, boolean isMuted) {
        }

        @Override
        public void onAdminAdded(String chatRoomId, String admin) {
            setChatRoomEvent(chatRoomId, EaseEvent.TYPE.CHAT_ROOM);
        }

        @Override
        public void onAdminRemoved(String chatRoomId, String admin) {
            setChatRoomEvent(chatRoomId, EaseEvent.TYPE.CHAT_ROOM);
        }

        @Override
        public void onOwnerChanged(String chatRoomId, String newOwner, String oldOwner) {
            setChatRoomEvent(chatRoomId, EaseEvent.TYPE.CHAT_ROOM);
        }

        @Override
        public void onAnnouncementChanged(String chatRoomId, String announcement) {
            setChatRoomEvent(chatRoomId, EaseEvent.TYPE.CHAT_ROOM);
        }
    }

    private void setChatRoomEvent(String roomId, EaseEvent.TYPE type) {
        EaseEvent easeEvent = new EaseEvent(DemoConstant.CHAT_ROOM_CHANGE, type);
        easeEvent.message = roomId;
        messageChangeLiveData.with(DemoConstant.CHAT_ROOM_CHANGE).postValue(easeEvent);
    }
}
