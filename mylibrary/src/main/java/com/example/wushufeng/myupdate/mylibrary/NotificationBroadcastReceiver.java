package com.example.wushufeng.myupdate.mylibrary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Message;
import android.widget.Toast;

import java.io.File;

/**
 * Created by Shufeng.Wu on 2016/12/21.
 */

public class NotificationBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals("notification_clicked")) {
            //处理点击事件
            Message.obtain(NetworkLib.myHandler, NetworkLib.CASE_APP_INSTALL_REPLACE).sendToTarget();

        }else if (action.equals("notification_cancelled")) {
            //处理滑动删除事件
        }
    }
}
