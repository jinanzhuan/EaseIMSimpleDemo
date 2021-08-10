package com.easemob.imdemo;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMChatManager;
import com.hyphenate.chat.EMChatRoomManager;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMContactManager;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMGroupManager;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.chat.EMPushManager;
import com.hyphenate.easeui.EaseIM;
import com.hyphenate.easeui.delegate.EaseCustomAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseExpressionAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseFileAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseImageAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseLocationAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseTextAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseVideoAdapterDelegate;
import com.hyphenate.easeui.delegate.EaseVoiceAdapterDelegate;
import com.hyphenate.easeui.domain.EaseAvatarOptions;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.manager.EaseMessageTypeSetManager;
import com.hyphenate.easeui.model.EaseNotifier;
import com.hyphenate.easeui.provider.EaseUserProfileProvider;
import com.hyphenate.push.EMPushHelper;
import com.hyphenate.push.EMPushType;
import com.hyphenate.push.PushListener;
import com.hyphenate.util.EMLog;

import java.util.Map;


/**
 * 作为hyphenate-sdk的入口控制类，获取sdk下的基础类均通过此类
 */
public class DemoHelper {
    private static final String TAG = DemoHelper.class.getSimpleName();

    public boolean isSDKInit;//SDK是否初始化
    private static DemoHelper mInstance;
    private Map<String, EaseUser> contactList;

    private Context mianContext;

    private String tokenUrl = "http://a1.easemob.com/token/rtcToken/v1";
    private String uIdUrl = "http://a1.easemob.com/channel/mapper";
    
    private Thread fetchUserTread;


    private DemoHelper() {}

    public static DemoHelper getInstance() {
        if(mInstance == null) {
            synchronized (DemoHelper.class) {
                if(mInstance == null) {
                    mInstance = new DemoHelper();
                }
            }
        }
        return mInstance;
    }

    public void init(Context context) {
        //初始化IM SDK
        if(initSDK(context)) {
            // debug mode, you'd better set it to false, if you want release your App officially.
            EMClient.getInstance().setDebugMode(true);
            //初始化推送
            initPush(context);
            //注册call Receiver
            //initReceiver(context);
            //初始化ease ui相关
            initEaseUI(context);
            //注册对话类型
            registerConversationType();
        }

    }

    /**
     * 初始化SDK
     * @param context
     * @return
     */
    private boolean initSDK(Context context) {
        // 根据项目需求对SDK进行配置
        EMOptions options = initChatOptions(context);
        //配置自定义的rest server和im server
        //options.setRestServer("a1-hsb.easemob.com");
        //options.setIMServer("106.75.100.247");
        //options.setImPort(6717);
        // 初始化SDK
        isSDKInit = EaseIM.getInstance().init(context, options);
        mianContext = context;
        return isSDKInit();
    }


    /**
     *注册对话类型
     */
    private void registerConversationType() {
        EaseMessageTypeSetManager.getInstance()
                .addMessageType(EaseExpressionAdapterDelegate.class)       //自定义表情
                .addMessageType(EaseFileAdapterDelegate.class)             //文件
                .addMessageType(EaseImageAdapterDelegate.class)            //图片
                .addMessageType(EaseLocationAdapterDelegate.class)         //定位
                .addMessageType(EaseVideoAdapterDelegate.class)            //视频
                .addMessageType(EaseVoiceAdapterDelegate.class)            //声音
                .addMessageType(EaseCustomAdapterDelegate.class)           //自定义消息
                .setDefaultMessageType(EaseTextAdapterDelegate.class);       //文本
    }

    /**
     * 判断是否之前登录过
     * @return
     */
    public boolean isLoggedIn() {
        return getEMClient().isLoggedInBefore();
    }

    /**
     * 获取IM SDK的入口类
     * @return
     */
    public EMClient getEMClient() {
        return EMClient.getInstance();
    }

    /**
     * 获取contact manager
     * @return
     */
    public EMContactManager getContactManager() {
        return getEMClient().contactManager();
    }

    /**
     * 获取group manager
     * @return
     */
    public EMGroupManager getGroupManager() {
        return getEMClient().groupManager();
    }

    /**
     * 获取chatroom manager
     * @return
     */
    public EMChatRoomManager getChatroomManager() {
        return getEMClient().chatroomManager();
    }


    /**
     * get EMChatManager
     * @return
     */
    public EMChatManager getChatManager() {
        return getEMClient().chatManager();
    }

    /**
     * get push manager
     * @return
     */
    public EMPushManager getPushManager() {
        return getEMClient().pushManager();
    }

    /**
     * get conversation
     * @param username
     * @param type
     * @param createIfNotExists
     * @return
     */
    public EMConversation getConversation(String username, EMConversation.EMConversationType type, boolean createIfNotExists) {
        return getChatManager().getConversation(username, type, createIfNotExists);
    }

    public String getCurrentUser() {
        return getEMClient().getCurrentUser();
    }

    /**
     * ChatPresenter中添加了网络连接状态监听，多端登录监听，群组监听，联系人监听，聊天室监听
     * @param context
     */
    private void initEaseUI(Context context) {
        //添加ChatPresenter,ChatPresenter中添加了网络连接状态监听，
        EaseIM.getInstance().addChatPresenter(ChatPresenter.getInstance());
        EaseIM.getInstance()
                .setAvatarOptions(getAvatarOptions())
                .setUserProvider(new EaseUserProfileProvider() {
                    @Override
                    public EaseUser getUser(String username) {
                        return getUserInfo(username);
                    }

                });
    }

    /**
     * 统一配置头像
     * @return
     */
    private EaseAvatarOptions getAvatarOptions() {
        EaseAvatarOptions avatarOptions = new EaseAvatarOptions();
        avatarOptions.setAvatarShape(1);
        return avatarOptions;
    }

    public EaseUser getUserInfo(String username) {
        // To get instance of EaseUser, here we get it from the user list in memory
        // You'd better cache it if you get it from your server
        return new EaseUser(username);
    }


    /**
     * 根据自己的需要进行配置
     * @param context
     * @return
     */
    private EMOptions initChatOptions(Context context){
        Log.d(TAG, "init HuanXin Options");

        EMOptions options = new EMOptions();
        // 设置是否自动接受加好友邀请,默认是true
        options.setAcceptInvitationAlways(false);
        // 设置是否需要接受方已读确认
        options.setRequireAck(true);
        // 设置是否需要接受方送达确认,默认false
        options.setRequireDeliveryAck(false);

        // 设置是否使用 fcm，有些华为设备本身带有 google 服务，

        /**
         * NOTE:你需要设置自己申请的账号来使用三方推送功能，详见集成文档
         */
//        EMPushConfig.Builder builder = new EMPushConfig.Builder(context);
//
//        builder.enableVivoPush() // 需要在AndroidManifest.xml中配置appId和appKey
//                .enableMeiZuPush("134952", "f00e7e8499a549e09731a60a4da399e3")
//                .enableMiPush("2882303761517426801", "5381742660801")
//                .enableOppoPush("0bb597c5e9234f3ab9f821adbeceecdb",
//                        "cd93056d03e1418eaa6c3faf10fd7537")
//                .enableHWPush() // 需要在AndroidManifest.xml中配置appId
//                .enableFCM("921300338324");
//        options.setPushConfig(builder.build());

        return options;
    }

    public void initPush(Context context) {
        if(EaseIM.getInstance().isMainProcess(context)) {
            //OPPO SDK升级到2.1.0后需要进行初始化
            //HMSPushHelper.getInstance().initHMSAgent(DemoApplication.getInstance());
            EMPushHelper.getInstance().setPushListener(new PushListener() {
                @Override
                public void onError(EMPushType pushType, long errorCode) {
                    // TODO: 返回的errorCode仅9xx为环信内部错误，可从EMError中查询，其他错误请根据pushType去相应第三方推送网站查询。
                    EMLog.e("PushClient", "Push client occur a error: " + pushType + " - " + errorCode);
                }
            });
        }
    }

    /**
     * logout
     *
     * @param unbindDeviceToken
     *            whether you need unbind your device token
     * @param callback
     *            callback
     */
    public void logout(boolean unbindDeviceToken, final EMCallBack callback) {
        Log.d(TAG, "logout: " + unbindDeviceToken);
        EMClient.getInstance().logout(unbindDeviceToken, new EMCallBack() {

            @Override
            public void onSuccess() {
                logoutSuccess();
                //reset();
                if (callback != null) {
                    callback.onSuccess();
                }

            }

            @Override
            public void onProgress(int progress, String status) {
                if (callback != null) {
                    callback.onProgress(progress, status);
                }
            }

            @Override
            public void onError(int code, String error) {
                Log.d(TAG, "logout: onSuccess");
                //reset();
                if (callback != null) {
                    callback.onError(code, error);
                }
            }
        });
    }

    /**
     * 退出登录后，需要处理的业务逻辑
     */
    public void logoutSuccess() {
        Log.d(TAG, "logout: onSuccess");
    }

    public EaseAvatarOptions getEaseAvatarOptions() {
        return EaseIM.getInstance().getAvatarOptions();
    }

    /**
     * get instance of EaseNotifier
     * @return
     */
    public EaseNotifier getNotifier(){
        return EaseIM.getInstance().getNotifier();
    }

    /**
     * 设置SDK是否初始化
     * @param init
     */
    public void setSDKInit(boolean init) {
        isSDKInit = init;
    }

    public boolean isSDKInit() {
        return isSDKInit;
    }


    /**
     * Determine if it is from the current user account of another device
     * @param username
     * @return
     */
    public boolean isCurrentUserFromOtherDevice(String username) {
        if(TextUtils.isEmpty(username)) {
            return false;
        }
        if(username.contains("/") && username.contains(EMClient.getInstance().getCurrentUser())) {
            return true;
        }
        return false;
    }
}
