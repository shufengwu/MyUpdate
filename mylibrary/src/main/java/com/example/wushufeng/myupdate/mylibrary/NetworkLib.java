package com.example.wushufeng.myupdate.mylibrary;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by wushufeng on 2016/12/19.
 */

public class NetworkLib {
    static Notification notifyBuilder;
    static NotificationCompat.Builder builder;
    static NotificationManager mNotificationManager;

    public static int notifyProgress = 0;
    public static int maxProgress = 0;

    public static ConnectivityManager mConnectivity;
    //检查网络连接
    public static NetworkInfo info;
    public static UpdateBean updateBean;
    public static ProgressDialog progressDialog = null;
    static MyHandler myHandler;


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

    //判断连接的网络是否为WIFI
    public static boolean isNetworkTypeWiFi(final Context context) {
        boolean res = false;

        if (isNetworkConnected(context)) {
            NetworkInfo info = mConnectivity.getActiveNetworkInfo();
            int netType = info.getType();
            int netSubtype = info.getSubtype();
            if (netType == ConnectivityManager.TYPE_WIFI) {  //WIFI
                res = true;
            } else {
                res = false;
            }
        }
        return res;
    }

    //检查更新
    public static void checkUpdate(final Context context) {
        myHandler = new MyHandler(context);
        new Thread() {
            @Override
            public void run() {
                URL url = null;
                try {
                    url = new URL("https://raw.githubusercontent.com/shufengwu/update_server/master/update.json");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    StringBuilder sb = new StringBuilder();
                    InputStream is = conn.getInputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) != -1) {
                        sb.append(new String(buffer, 0, length));
                    }
                    System.out.println(sb.toString());
                    Gson gson = new Gson();
                    updateBean = gson.fromJson(sb.toString(), UpdateBean.class);

                    if (Integer.parseInt(updateBean.getVersionCode()) > getVersionCode(context)) {
                        if (NetworkLib.isNetworkTypeWiFi(context)) {
                            Message.obtain(myHandler, 0).sendToTarget();
                        } else {
                            Message.obtain(myHandler, 1).sendToTarget();
                        }
                    }

                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    public static void downNewApk(final String urlStr, Context context) {
        //显示ProgressDialog
        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle("更新");
        progressDialog.setIcon(android.R.drawable.ic_dialog_alert);
        progressDialog.setMessage("正在下载更新...");
        progressDialog.show();
        showNotification(context);
        new Thread() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    maxProgress = conn.getContentLength();
                    progressDialog.setMax(maxProgress);
                    InputStream inputStream = conn.getInputStream();
                    File file = new File(Environment.getExternalStorageDirectory(), "updata.apk");
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(inputStream);
                    byte[] buffer = new byte[1024];
                    int len;
                    int total = 0;
                    while ((len = bis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        total += len;
                        //获取当前下载量
                        Message.obtain(myHandler, 3, total).sendToTarget();
                    }
                    fos.close();
                    bis.close();
                    inputStream.close();
                    Message.obtain(myHandler, 2).sendToTarget();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    public static class MyHandler extends Handler {
        private Context context;

        public MyHandler(Context context) {
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //wifi下更新提示对话框
                case 0:

                    new AlertDialog.Builder(context)
                            .setTitle("提示")
                            .setMessage("当前版本为:" + getVersionName(context) + " Code:" + getVersionCode(context)
                                    + "\n发现新版本:" + updateBean.getVersion() + " Code:" + updateBean.getVersionCode()
                                    + "\n更新日志:"
                                    + "\n" + updateBean.getDescription())
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("更新", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    downNewApk(updateBean.getUrl(), context);
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
                //数据网络下更新提示对话框
                case 1:
                    new AlertDialog.Builder(context)
                            .setTitle("提示")
                            .setMessage("当前版本为:" + getVersionName(context) + " Code:" + getVersionCode(context)
                                    + "\n发现新版本:" + updateBean.getVersion() + " Code:" + updateBean.getVersionCode()
                                    + "\n更新日志:"
                                    + "\n" + updateBean.getDescription())
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("更新", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    new AlertDialog.Builder(context)
                                            .setTitle("提示")
                                            .setMessage("当前网络处于非WiFi网络状态下，使用移动网络下载更新？")
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    downNewApk(updateBean.getUrl(), context);
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
                case 2:
                    Toast.makeText(context, "下载更新成功", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    new AlertDialog.Builder(context)
                            .setTitle("提示")
                            .setMessage("更新下载完成，是否现在安装并替换本应用？")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    File file = new File(Environment.getExternalStorageDirectory(), "updata.apk");
                                    installApk(context, file);
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
                case 3:
                    int p = (int) msg.obj;
                    progressDialog.setProgress(p);
                    progressDialog.setProgressNumberFormat(getChangedContentLength(p) + "/" + getChangedContentLength(progressDialog.getMax()));
                    builder.setProgress(100, getPercentProgress(notifyProgress), false);
                    mNotificationManager.notify(0, builder.build());
                    break;
            }
        }
    }

    public static String getVersionName(Context context) {
        //获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        //getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packInfo.versionName;
    }

    public static int getVersionCode(Context context) {
        //获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packInfo.versionCode;
    }

    //apk文件大小换算+单位
    static String getChangedContentLength(int progress) {
        String res = null;
        if (progress < 1024) {
            res = progress + " B";
            //1024 * 1024
        } else if (progress < 1048576) {
            res = new BigDecimal((double) progress / 1024).setScale(2, BigDecimal.ROUND_DOWN) + " KB";
        } else {
            res = new BigDecimal((double) progress / 1024 / 1024).setScale(2, BigDecimal.ROUND_DOWN) + " MB";
        }
        return res;
    }

    static int getPercentProgress(int progress){
        int res = 0;
        String tmp = new BigDecimal((double) progress / maxProgress).setScale(2, BigDecimal.ROUND_DOWN)+"";
        res = (int)Double.parseDouble(tmp);
        return res;
    }

    //安装apk
    public static void installApk(Context context, File file) {
        Intent intent = new Intent();
        //执行动作
        intent.setAction(Intent.ACTION_VIEW);
        //执行的数据类型
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        context.startActivity(intent);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public static void showNotification(Context context) {

        builder= new NotificationCompat.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        builder.setAutoCancel(false);
        builder.setOngoing(false);
        builder.setShowWhen(true);
        builder.setContentTitle("正在下载更新... " + notifyProgress + "%");
        builder.setProgress(100, notifyProgress, false);
        notifyBuilder = builder.build();
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, notifyBuilder);

    }


}
