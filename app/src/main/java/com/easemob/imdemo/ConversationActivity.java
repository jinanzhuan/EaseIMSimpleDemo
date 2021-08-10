package com.easemob.imdemo;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.easemob.imdemo.permission.PermissionsManager;
import com.easemob.imdemo.permission.PermissionsResultAction;
import com.hyphenate.EMCallBack;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.widget.EaseTitleBar;
import com.hyphenate.util.EMLog;

import java.util.List;

public class ConversationActivity extends BaseActivity {
    private EaseTitleBar titleBar;

    public static void actionStart(Context context) {
        context.startActivity(new Intent(context, ConversationActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        titleBar = findViewById(R.id.title_bar);
        titleBar.setOnBackPressListener(new EaseTitleBar.OnBackPressListener() {
            @Override
            public void onBackPress(View view) {
                onBackPressed();
            }
        });
        titleBar.setTitle("会话列表");

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fl_fragment, new ConversationListFragment())
                .commit();

        LiveDataBus.get().with(DemoConstant.ACCOUNT_CHANGE, EaseEvent.class).observe(this, new Observer<EaseEvent>() {
            @Override
            public void onChanged(EaseEvent event) {
                if(event == null) {
                    return;
                }
                if(!event.isAccountChange()) {
                    return;
                }
                String accountEvent = event.event;
                if(TextUtils.equals(accountEvent, DemoConstant.ACCOUNT_REMOVED) ||
                        TextUtils.equals(accountEvent, DemoConstant.ACCOUNT_KICKED_BY_CHANGE_PASSWORD) ||
                        TextUtils.equals(accountEvent, DemoConstant.ACCOUNT_KICKED_BY_OTHER_DEVICE)) {
                    DemoHelper.getInstance().logout(false, new EMCallBack() {
                        @Override
                        public void onSuccess() {
                            finishOtherActivities();
                            startActivity(new Intent(ConversationActivity.this, MainActivity.class));
                            finish();
                        }

                        @Override
                        public void onError(int code, String error) {
                            EMLog.e("logout", "logout error: error code = "+code + " error message = "+error);
                            //showToast("logout error: error code = "+code + " error message = "+error);
                        }

                        @Override
                        public void onProgress(int progress, String status) {

                        }
                    });
                }
            }
        });
        checkPermission();
    }

    private void checkPermission() {
        PermissionsManager.getInstance()
                .requestAllManifestPermissionsIfNecessary(this, new PermissionsResultAction() {
                    @Override
                    public void onGranted() {

                    }

                    @Override
                    public void onDenied(String permission) {

                    }
                });
    }

    /**
     * 结束除了当前Activity外的其他Activity
     */
    protected void finishOtherActivities() {
        UserActivityLifecycleCallbacks lifecycleCallbacks = DemoApplication.getInstance().getLifecycleCallbacks();
        if(lifecycleCallbacks == null) {
            finish();
            return;
        }
        List<Activity> activities = lifecycleCallbacks.getActivityList();
        if(activities == null || activities.isEmpty()) {
            finish();
            return;
        }
        for(Activity activity : activities) {
            if(activity != lifecycleCallbacks.current()) {
                activity.finish();
            }
        }
    }
}
