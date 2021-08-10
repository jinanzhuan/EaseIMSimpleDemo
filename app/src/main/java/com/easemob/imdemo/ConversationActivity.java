package com.easemob.imdemo;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.hyphenate.easeui.widget.EaseTitleBar;

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
    }
}
