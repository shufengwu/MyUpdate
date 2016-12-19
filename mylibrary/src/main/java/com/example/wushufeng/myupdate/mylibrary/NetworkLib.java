package com.example.wushufeng.myupdate.mylibrary;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;

import com.google.gson.Gson;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by wushufeng on 2016/12/19.
 */

public class NetworkLib {

    //public static final int WIFI
    public static ConnectivityManager mConnectivity;
    //检查网络连接
    public static NetworkInfo info;

    //判断是否联网
    public static boolean isNetworkConnected(final Context context) {
        boolean res = false;
        mConnectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //检查网络连接
        info = mConnectivity.getActiveNetworkInfo();
        if (info == null || !mConnectivity.getBackgroundDataSetting()) {
            res = false;
        } else {
            res = true;
        }
        return res;
    }


    public static boolean isNetworkTypeWiFi(final Context context){
        boolean res = false;

        if(isNetworkConnected(context)){
            NetworkInfo info = mConnectivity.getActiveNetworkInfo();
            int netType = info.getType();
            int netSubtype = info.getSubtype();
            if (netType == ConnectivityManager.TYPE_WIFI) {  //WIFI
                res = true;
            }else{
                res = false;
            }
        }
        return res;
    }

    public static void checkUpdate(final Context context){
        final MyHandler myHandler = new MyHandler(context);
        new Thread(){
            @Override
            public void run() {

                URL url = null;
                try {

                    url = new URL("https://raw.githubusercontent.com/shufengwu/update_server/master/update.json");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    StringBuilder sb = new StringBuilder();
                    InputStream is = conn.getInputStream();
                    Message.obtain(myHandler,2).sendToTarget();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) != -1) {
                        sb.append(new String(buffer, 0, length));
                    }
                    Message.obtain(myHandler,2).sendToTarget();
                    System.out.println(sb.toString());
                    Gson gson = new Gson();
                    UpdateBean updateBean = gson.fromJson(sb.toString(),UpdateBean.class);
                    if(!updateBean.getVersion().equals(NetworkLib.getVersionName(context))){
                        if (NetworkLib.isNetworkTypeWiFi(context)){
                            Message.obtain(myHandler,0).sendToTarget();
                        }else{
                            Message.obtain(myHandler,1).sendToTarget();
                        }
                    }

                } catch (java.io.IOException e) {
                    e.printStackTrace();
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

            }
        }.start();

    }


    public static class MyHandler extends Handler {
        private Context context;
        public MyHandler(Context context){
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 0:
                    new AlertDialog.Builder(context)
                            .setTitle("提示")
                            .setMessage("当前版本为：")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("更新", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .create()
                            .show();
                    break;
                case 1:
                    new AlertDialog.Builder(context)
                            .setTitle("提示")
                            .setMessage("当前网络处于非WiFi网络状态下，使用移动网络下载更新？")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    new AlertDialog.Builder(context)
                                            .setTitle("提示")
                                            .setMessage("当前版本为：")
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .setPositiveButton("更新", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    dialogInterface.dismiss();
                                                }
                                            })
                                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    dialogInterface.dismiss();
                                                }
                                            })
                                            .create()
                                            .show();
                                    dialogInterface.dismiss();
                                }
                            })
                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .create()
                            .show();
                    break;
            }
        }
    }

    public static String getVersionName(Context context) throws PackageManager.NameNotFoundException {
        //获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        //getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        return packInfo.versionName;
    }

    public static int getVersionCode(Context context) throws PackageManager.NameNotFoundException {
        //获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        return packInfo.versionCode;
    }

}
