package com.example.wushufeng.myupdate;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.wushufeng.myupdate.mylibrary.NetworkLib;

public class MainActivity extends AppCompatActivity {

    private final String urlStr = "http://172.22.35.177:8081/update.json";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NetworkLib.checkUpdate(this,urlStr);

    }


}
