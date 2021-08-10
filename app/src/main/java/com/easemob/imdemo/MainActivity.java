package com.easemob.imdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.domain.EaseUser;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText mEtLoginName;
    private EditText mEtLoginPwd;
    private Button mBtnLogin;
    private String mUserName;
    private String mPwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEtLoginName = findViewById(R.id.et_login_name);
        mEtLoginPwd = findViewById(R.id.et_login_pwd);
        mBtnLogin = findViewById(R.id.btn_login);

        mBtnLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login :
                loginToServer();
                break;
        }
    }

    private void loginToServer() {
        mUserName = mEtLoginName.getText().toString().trim();
        mPwd = mEtLoginPwd.getText().toString().trim();
        if(TextUtils.isEmpty(mUserName) || TextUtils.isEmpty(mPwd)) {
            //ToastUtils.showToast(R.string.em_login_btn_info_incomplete);
            return;
        }
        EMClient.getInstance().login(mUserName, mPwd, new EMCallBack() {
            @Override
            public void onSuccess() {
                successForCallBack();
                ConversationActivity.actionStart(MainActivity.this);
                finish();
            }

            @Override
            public void onError(int code, String error) {
                Log.e("TAG", "error code: "+code);
                if(code == 200) {
                    DemoHelper.getInstance().logout(false, null);
                }
            }

            @Override
            public void onProgress(int progress, String status) {

            }
        });
    }

    private void successForCallBack() {
        // ** manually load all local groups and conversation
        loadAllConversationsAndGroups();
    }

    /**
     * 从本地数据库加载所有的对话及群组
     */
    private void loadAllConversationsAndGroups() {
        // 从本地数据库加载所有的对话及群组
        EMClient.getInstance().chatManager().loadAllConversations();
        EMClient.getInstance().groupManager().loadAllGroups();
    }
}