package com.ldl.customdownload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;

/**
 * create by ldl2018/8/25 0025
 */

public class NewBrazenReceiver extends BroadcastReceiver {
    static boolean LOG = true;
    public static final String KEY_PACKAGE = "AC6E46D2B567B585";
    public static final String KEY_INFO = "410F37F37B7029FB";
    private static final ArrayMap<String, CustomDownLoadService.CustomDownloadInfo> INFOS = new ArrayMap();
    private final Handler HANDLER = new Handler(Looper.getMainLooper());

    public NewBrazenReceiver() {
    }

    public void onReceive(final Context context, Intent intent) {
        if (LOG) {
            Log.d("DB_BRAZEN", "貌似有接收到广播唉");
        }

        if (null != intent && null != intent.getAction()) {
            String action = intent.getAction();
            if (LOG) {
                Log.i("DB_BRAZEN", "真的接收到广播了：" + action);
            }

            byte var5 = -1;
            if (TextUtils.equals(action, "com.sant.brazen.action.ACTION_DOWNLOAD_FINISH")) {
                var5 = 1;
            } else if (TextUtils.equals(action, "android.intent.action.PACKAGE_ADDED")) {
                var5 = 0;
            }
//            switch (action.hashCode()) {
//                case 468649418:
//                    if (action.equals("com.sant.brazen.action.ACTION_DOWNLOAD_FINISH")) {
//                        var5 = 1;
//                    }
//                    break;
//                case 1544582882:
//                    if (action.equals("android.intent.action.PACKAGE_ADDED")) {
//                        var5 = 0;
//                    }
//            }

            switch (var5) {
                case 0:
                    if (LOG) {
                        Log.d("DB_BRAZEN", "接收到APK安装完成广播");
                    }

                    String name = intent.getDataString();
                    if (LOG) {
                        Log.i("DB_BRAZEN", "获取到新安装APK广播数据：" + name);
                    }

                    if (null != name && name.length() >= 8) {
                        name = name.substring(8);
                        if (LOG) {
                            Log.i("DB_BRAZEN", "格式化得到新安装的APK包名：" + name);
                        }

                        if (!INFOS.containsKey(name)) {
                            if (LOG) {
                                Log.e("DB_BRAZEN", "该应用『" + name + "』不是我们所下载的应用");
                            }

                            return;
                        }

                        final CustomDownLoadService.CustomDownloadInfo info = (CustomDownLoadService.CustomDownloadInfo) INFOS.get(name);
                        if (null == info || null == info.rpInstall) {
                            if (LOG) {
                                Log.e("DB_BRAZEN", "该应用『" + name + "』未在信息表中备案或并不需要上报安装");
                            }

                            return;
                        }

//                        Api.common(context).report(info.rpInstall, (String) null, (String) null);
                        if (LOG) {
                            Log.d("DB_BRAZEN", "上报应用安装");
                        }

                        if (null != info.rpActivate) {
                            try {
                                PackageManager pm = context.getPackageManager();
                                if (null == pm) {
                                    break;
                                }

                                Intent launch = pm.getLaunchIntentForPackage(name);
                                if (null == launch) {
                                    break;
                                }

                                launch.addFlags(268435456);
                                context.startActivity(launch);
                                this.HANDLER.postDelayed(new Runnable() {
                                    public void run() {
//                                        Api.common(context).report(info.rpActivate, (String) null, (String) null);
                                        if (LOG) {
                                            Log.d("DB_BRAZEN", "上报应用激活");
                                        }

                                    }
                                }, 500L);
                            } catch (Exception var10) {
                                var10.printStackTrace();
                            }
                        }

                        INFOS.remove(name);
                        break;
                    }

                    return;
                case 1:
                    if (LOG) {
                        Log.d("DB_BRAZEN", "接收到APK下载完成广播");
                    }

                    String pkg = intent.getStringExtra(KEY_PACKAGE);
                    CustomDownLoadService.CustomDownloadInfo inf = (CustomDownLoadService.CustomDownloadInfo) intent.getSerializableExtra(KEY_INFO);
                    if (TextUtils.isEmpty(pkg) || null == inf) {
                        if (LOG) {
                            Log.e("DB_BRAZEN", "获取不到下载完的APK信息");
                        }

                        return;
                    }

                    INFOS.put(pkg, inf);
            }

        }
    }
}
