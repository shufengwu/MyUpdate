package com.example.wushufeng.myupdate.mylibrary;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by wushufeng on 2016/12/19.
 */

public class NetworkLib {
    private static Notification notifyBuilder;
    private static NotificationCompat.Builder builder;
    private static NotificationManager mNotificationManager;

    private static int notifyProgress = 0;
    private static int maxProgress = 0;

    private static ConnectivityManager mConnectivity;
    //检查网络连接
    public static NetworkInfo info;
    private static UpdateBean updateBean;
    private static ProgressDialog progressDialog = null;
    public static MyHandler myHandler;
    private static int updateCount_pBar = 0;
    private static int updateCount_nofi = 0;
    private static AlertDialog alertDialog_case2 = null;

    private static final int CASE_WIFI_SHOW_DIALOG = 0;
    private static final int CASE_DATA_NETWORK_SHOW_DIALOG = 1;
    public static final int CASE_APP_INSTALL_REPLACE = 2;
    private static final int CASE_PROGRESS_UPDATE = 3;
    private static final int CASE_NOTI_PROGRESS_UPDATE = 4;
    private static final int CASE_DATA_NETWORK_DOWNLOAD_DIALOG = 5;
    private static final String POSI_BUTTON_UPDATE_LOG = "更新";
    private static final String POSI_BUTTON_OTHER = "确定";
    private static final String NEGA_BUTTON_CANCEL = "取消";


    //判断是否有可用网络
    private static boolean isNetworkConnected(final Context context) {
        boolean res = true;
        //ConnectivityManager主要管理和网络连接相关的操作
        //Class that answers queries about the state of network connectivity. It also notifies applications when network connectivity changes.
        mConnectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        //Returns details about the currently active default data network.
        info = mConnectivity.getActiveNetworkInfo();

        if (info == null || !info.isAvailable()) {
            res = false;
        }
        return res;
    }

    //判断连接的网络是否为WIFI
    private static boolean isNetworkTypeWiFi(final Context context) {
        boolean res = false;
        if (isNetworkConnected(context)) {
            NetworkInfo info = mConnectivity.getActiveNetworkInfo();
            //Reports the type of network to which the info in this NetworkInfo pertains.
            int netType = info.getType();
            if (netType == ConnectivityManager.TYPE_WIFI) {
                res = true;
            }
        }
        return res;
    }

    //检查更新
    public static void checkUpdate(final Context context, final String urlStr) {
        myHandler = new MyHandler(context);
        new Thread() {
            @Override
            public void run() {
                URL url;
                try {
                    url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    StringBuilder sb = new StringBuilder();
                    InputStream is = conn.getInputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) != -1) {
                        sb.append(new String(buffer, 0, length, "GB2312"));
                    }
                    updateBean = parseJsonToUpdateBean(sb.toString());

                    if (Integer.parseInt(updateBean.getVersionCode()) > PkgInfo.getVersionCode(context)) {
                        if (NetworkLib.isNetworkTypeWiFi(context)) {
                            Message.obtain(myHandler, CASE_WIFI_SHOW_DIALOG).sendToTarget();
                        } else {
                            Message.obtain(myHandler, CASE_DATA_NETWORK_SHOW_DIALOG).sendToTarget();
                        }
                    }

                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private static void downNewApk(final String urlStr, Context context) {
        //显示ProgressDialog
        showProgerssDialog(context);
        new Thread() {
            @Override
            public void run() {
                try {
                    Log.e("NetworkLib","-----------------------1");
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    maxProgress = conn.getContentLength();
                    progressDialog.setMax(maxProgress);
                    Log.e("NetworkLib","-----------------------2");
                    InputStream inputStream = conn.getInputStream();
                    File file = new File(Environment.getExternalStorageDirectory(), "updata.apk");
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(inputStream);
                    Log.e("NetworkLib","-----------------------3");
                    byte[] buffer = new byte[1024];
                    int len;
                    int total = 0;
                    Log.e("NetworkLib","-----------------------4");
                    while ((len = bis.read(buffer)) != -1) {
                        Log.e("NetworkLib","-----------------------5");
                        fos.write(buffer, 0, len);
                        total += len;
                        //获取当前下载量,更新progressdialog
                        Message.obtain(myHandler, CASE_PROGRESS_UPDATE, total).sendToTarget();
                        //更新notification
                        Message.obtain(myHandler, CASE_NOTI_PROGRESS_UPDATE, total).sendToTarget();
                    }
                    fos.close();
                    bis.close();
                    inputStream.close();
                    Message.obtain(myHandler, CASE_APP_INSTALL_REPLACE).sendToTarget();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    private static UpdateBean parseJsonToUpdateBean(String jsonStr) {
        UpdateBean res = new UpdateBean();
        JSONObject jo;
        try {
            jo = new JSONObject(jsonStr);
            res.setVersion(jo.getString("version"));
            res.setVersionCode(jo.getString("versionCode"));
            res.setUrl(jo.getString("url"));
            res.setDescription(jo.getString("description"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return res;
    }

    private static class MyHandler extends Handler {
        private Context context;

        MyHandler(Context context) {
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //wifi下更新提示对话框
                case CASE_WIFI_SHOW_DIALOG:
                    String message_wifi = "当前版本为:" + PkgInfo.getVersionName(context) + " Code:" + PkgInfo.getVersionCode(context)
                            + "\n发现新版本:" + updateBean.getVersion() + " Code:" + updateBean.getVersionCode()
                            + "\n更新日志:"
                            + "\n" + updateBean.getDescription();
                    new AlertDialog.Builder(context)
                            .setTitle("提示")
                            .setMessage(message_wifi)
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setPositiveButton(POSI_BUTTON_UPDATE_LOG, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    updateCount_pBar = 0;
                                    updateCount_nofi = 0;
                                    showNotification(context);
                                    downNewApk(updateBean.getUrl(), context);
                                    dialogInterface.dismiss();
                                }
                            })
                            .setNegativeButton(NEGA_BUTTON_CANCEL, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .create()
                            .show();


                    break;
                //数据网络下更新提示对话框
                case CASE_DATA_NETWORK_SHOW_DIALOG:
                    String message_data = "当前版本为:" + PkgInfo.getVersionName(context) + " Code:" + PkgInfo.getVersionCode(context)
                            + "\n发现新版本:" + updateBean.getVersion() + " Code:" + updateBean.getVersionCode()
                            + "\n更新日志:"
                            + "\n" + updateBean.getDescription();
                    new AlertDialog.Builder(context)
                            .setTitle("提示")
                            .setMessage(message_data)
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setPositiveButton(POSI_BUTTON_UPDATE_LOG, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Message.obtain(myHandler, CASE_DATA_NETWORK_DOWNLOAD_DIALOG).sendToTarget();
                                    dialogInterface.dismiss();
                                }
                            })
                            .setNegativeButton(NEGA_BUTTON_CANCEL, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .create()
                            .show();


                    break;
                case CASE_APP_INSTALL_REPLACE:
                    progressDialog.dismiss();
                    //防止重复显示
                    if (alertDialog_case2 != null && alertDialog_case2.isShowing()) {
                        alertDialog_case2.dismiss();
                    }
                    AlertDialog.Builder updateBuilder = new AlertDialog.Builder(context)
                            .setTitle("提示")
                            .setMessage("更新下载完成，是否现在安装并替换本应用？")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setPositiveButton(POSI_BUTTON_OTHER, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    File file = new File(Environment.getExternalStorageDirectory(), "updata.apk");
                                    installApk(context, file);
                                    dialogInterface.dismiss();
                                }
                            })
                            .setNegativeButton(NEGA_BUTTON_CANCEL, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });
                    alertDialog_case2 = updateBuilder.create();
                    alertDialog_case2.show();

                    break;
                case CASE_PROGRESS_UPDATE:
                    int p1 = (int) msg.obj;
                    //至少增加10%再更新
                    if (p1 / (maxProgress / 10) >= updateCount_pBar) {
                        updateCount_pBar += 1;
                        progressDialog.setProgress(p1);
                        progressDialog.setProgressNumberFormat(NumTrans.getChangedContentLength(p1) + "/" + NumTrans.getChangedContentLength(progressDialog.getMax()));
                    }
                    break;
                case CASE_NOTI_PROGRESS_UPDATE:
                    //至少增加10%再更新
                    int p2 = (int) msg.obj;
                    if (p2 / (maxProgress / 10) >= updateCount_nofi) {
                        updateCount_nofi += 1;
                        notifyProgress = NumTrans.getPercentProgress(p2, maxProgress);
                        builder.setContentTitle("正在下载更新... " + notifyProgress + "%");
                        builder.setProgress(100, notifyProgress, false);
                        if (notifyProgress == 100) {
                            builder.setContentTitle("下载完成 " + notifyProgress + "%");
                        }
                        mNotificationManager.notify(0, builder.build());
                    }

                    break;
                case CASE_DATA_NETWORK_DOWNLOAD_DIALOG:
                    new AlertDialog.Builder(context)
                            .setTitle("提示")
                            .setMessage("当前网络处于非WiFi网络状态下，使用移动网络下载更新？")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setPositiveButton(POSI_BUTTON_OTHER, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    updateCount_pBar = 0;
                                    updateCount_nofi = 0;
                                    showNotification(context);
                                    downNewApk(updateBean.getUrl(), context);
                                    dialogInterface.dismiss();
                                }
                            })
                            .setNegativeButton(NEGA_BUTTON_CANCEL, new DialogInterface.OnClickListener() {
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


    //安装apk
    private static void installApk(Context context, File file) {
        Intent intent = new Intent();
        //执行动作
        intent.setAction(Intent.ACTION_VIEW);
        //执行的数据类型
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        context.startActivity(intent);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    //显示ProgerssDialog
    private static void showProgerssDialog(Context context) {
        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle("更新");
        progressDialog.setIcon(android.R.drawable.ic_dialog_info);
        progressDialog.setMessage("正在下载更新...");
        progressDialog.show();
    }

    //static

    //显示Notification
    private static void showNotification(Context context) {
        Intent intentClick = new Intent(context, NotificationBroadcastReceiver.class);
        intentClick.setAction("notification_clicked");
        PendingIntent pendingIntentClick = PendingIntent.getBroadcast(context, 0, intentClick, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentCancel = new Intent(context, NotificationBroadcastReceiver.class);
        intentCancel.setAction("notification_cancelled");
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(context, 0, intentCancel, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        builder.setAutoCancel(true);
        builder.setOngoing(false);
        builder.setShowWhen(true);
        builder.setContentTitle("正在下载更新... " + notifyProgress + "%");
        builder.setProgress(100, notifyProgress, false);
        builder.setContentIntent(pendingIntentClick);
        builder.setDeleteIntent(pendingIntentCancel);
        notifyBuilder = builder.build();
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, notifyBuilder);

    }
}


