package com.example.administrator.myhttp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String url="http://........";
        //工具类的试用:执行异步get请求,其他方法暂不测试
        OkHttpHelper.create().execGet(url, new OkHttpHelper.HttpCallback() {
            @Override
            public void onSuccess(String data) {
                Toast.makeText(MainActivity.this, data, Toast.LENGTH_SHORT).show();
                //刷新UI
            }
            @Override
            public void onFail(Exception e) {
            }
        });
    }
}
