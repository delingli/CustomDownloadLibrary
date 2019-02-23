package com.ldl.customdownload;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * create by ldl2018/8/20 0020
 */

public class CustomDownLoadService extends IntentService {

    static final String ACTION_DOWNLOAD_FINISH = "com.sant.brazen.action.ACTION_DOWNLOAD_FINISH";
    private static final File DIR;
    public static final String EXTRA_INFO = "D54012286FECE209";
    private static boolean isDebug = true;
    private final List<String> DOWNLOADING = new ArrayList();
    private final X509TrustManager TRUST = new X509TrustManager() {
        @SuppressLint({"TrustAllX509TrustManager"})
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @SuppressLint({"TrustAllX509TrustManager"})
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };
    //    private NotificationManagerCompat NMC;
    private NotificationManager NMC;

    public CustomDownLoadService() {
        super("Custom_DOWNLOAD_SERVICE");
        if (!DIR.exists() && DIR.mkdirs() && isDebug) {
            Log.d("DB_BRAZEN", "创建下载目录成功");
        }

    }

    public static void start(Context context, CustomDownloadInfo info) {
        Intent intent = new Intent(context, CustomDownLoadService.class);
        intent.putExtra(EXTRA_INFO, info);
        if (isDebug) {
            Log.d("DB_BRAZEN", "即将开始启动下载任务");
        }

        context.startService(intent);
        Toast.makeText(context, "开启下载", Toast.LENGTH_SHORT).show();
    }

    public void onCreate() {
        super.onCreate();
        this.NMC = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    protected void onHandleIntent(Intent intent) {
        if (null == intent) {
            if (isDebug) {
                Log.e("DB_BRAZEN", "下载服务获取到空的意图");
            }

        } else {
            Serializable serializableExtra = intent.getSerializableExtra(EXTRA_INFO);
            CustomDownloadInfo info = null;
            if (serializableExtra instanceof CustomDownloadInfo) {
                info = (CustomDownloadInfo) serializableExtra;
            }
            if (null == info) {
                if (isDebug) {
                    Log.e("DB_BRAZEN", "下载服务获取不到下载对象");
                }
                return;

            } else if (TextUtils.isEmpty(info.link)) {
                if (isDebug) {
                    Log.e("DB_BRAZEN", "下载服务获取到空的链接地址");
                }

            } else if (this.DOWNLOADING.contains(info.link)) {
                if (isDebug) {
                    Log.e("DB_BRAZEN", "该文件正在下载");
                }

            } else {
                this.DOWNLOADING.add(info.link);
                if (isDebug) {
                    Log.i("DB_BRAZEN", "下载服务获取到链接地址：" + info.link);
                }
                String channelId = "channelId";
                Notification.Builder mBuilder = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    createNotificationChannel(channelId, "aaa",NotificationManager.IMPORTANCE_DEFAULT);
                    NotificationChannel channel = new NotificationChannel(channelId, "ldl", NotificationManager.IMPORTANCE_DEFAULT);
                    channel.enableLights(true); //是否在桌面icon右上角展示小红点
                    channel.setLightColor(Color.GREEN); //小红点颜色
                    channel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
                    NMC.createNotificationChannel(channel);
                    mBuilder = new Notification.Builder(this, channel.getId());

                } else {
                    mBuilder = new Notification.Builder(this);
                }
                RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.custom_notification_remoteview);
                mBuilder.setSmallIcon(R.drawable.ic_download);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mBuilder.setPriority(Notification.PRIORITY_MAX);
                }
                contentView.setTextViewText(R.id.tv_appProgress, "0%");
                contentView.setProgressBar(R.id.pb_downloadProgress, 100, 0, false);
                contentView.setImageViewResource(R.id.iv_appIcon, R.drawable.ic_download);
                mBuilder.setWhen(0L);
                mBuilder.setOngoing(true);
                mBuilder.setContent(contentView);
                if (null != info.rpStart) {
//                    Api.common(this).report(info.rpStart, (String) null, (String) null);
                    if (isDebug) {
                        Log.d("DB_BRAZEN", "上报下载开始");
                    }
                }

                String redirect = info.link;
                int count = 0;
                downLoadPic(info.iconurl, contentView);
                while (true) {
                    if (count < 5) {
                        URL link = null;
                        try {
                            link = new URL(redirect);
                        } catch (MalformedURLException var34) {
                            var34.printStackTrace();
                        }

                        if (null == link) {
                            if (isDebug) {
                                Log.e("DB_BRAZEN", "下载服务无法识别该下载地址：" + info.link);
                            }

                            return;
                        }

                        String protocol = link.getProtocol();
                        if (TextUtils.isEmpty(protocol)) {
                            if (isDebug) {
                                Log.e("DB_BRAZEN", "无法判断当前下载地址的协议头");
                            }

                            return;
                        }

                        String scheme = protocol.toLowerCase();
                        if (isDebug) {
                            Log.i("DB_BRAZEN", "当前下载地址协议头为：" + protocol + "，转为全小写后：" + scheme);
                        }


                        HttpURLConnection conn = null;
                        byte var11 = -1;
                        switch (scheme.hashCode()) {
                            case 3213448:
                                if (scheme.equals("http")) {
                                    var11 = 0;
                                }
                                break;
                            case 99617003:
                                if (scheme.equals("https")) {
                                    var11 = 1;
                                }
                        }

                        switch (var11) {
                            case 0:
                                if (isDebug) {
                                    Log.i("DB_BRAZEN", "当前下载通过HTTP下载");
                                }

                                conn = this.buildConnWithHttp(link);
                                break;
                            case 1:
                                if (isDebug) {
                                    Log.i("DB_BRAZEN", "当前下载通过HTTPS下载");
                                }

                                conn = this.buildConnWithHttps(link);
                        }

                        if (null == conn) {
                            if (isDebug) {
                                Log.e("DB_BRAZEN", "无法通过下载地址获得连接对象");
                            }

                            return;
                        }

                        conn.setConnectTimeout(1000 * 30);
                        conn.setReadTimeout(1000 * 30);
                        conn.setAllowUserInteraction(true);

                        try {
                            conn.setRequestMethod("GET");
                        } catch (ProtocolException var33) {
                            var33.printStackTrace();
                        }

                        int status = -1;

                        try {
                            status = conn.getResponseCode();
                        } catch (IOException var32) {
                            var32.printStackTrace();
                        }

                        if (status == -1) {
                            if (isDebug) {
                                Log.e("DB_BRAZEN", "获取服务器响应码失败");
                            }

                            return;
                        }
                        int remainder = status / 100;
                        if (isDebug) {
                            Log.i("DB_BRAZEN", "获取到服务器响应码：" + status + ", 计算后得到响应类型：" + remainder);
                        }

                        switch (remainder) {
                            case 2:
                                if (isDebug) {
                                    Log.d("DB_BRAZEN", "服务器响应成功");
                                }

                                InputStream is = null;

                                try {
                                    is = conn.getInputStream();
                                } catch (IOException var31) {
                                    var31.printStackTrace();
                                }

                                if (null == is) {
                                    if (isDebug) {
                                        Log.e("DB_BRAZEN", "无法从服务器获得输入流");
                                    }

                                    return;
                                }

                                String name = this.buildFileName(info, conn);
                                if (!TextUtils.isEmpty(name)) {
                                    StringBuilder sb = new StringBuilder();

                                    if (name.length() < 14) {
                                        contentView.setTextViewText(R.id.tv_appName, name);
                                    } else {

                                        char[] chars = name.toCharArray();
                                        for (int i = 0; i < name.length(); ++i) {
                                            if (i < 7) {
                                                sb.append(chars[i]);
                                            }
                                        }
                                        String nn = name.substring(name.lastIndexOf(".") - 3);
                                        String newname = sb.toString() + "..." + nn;
                                        contentView.setTextViewText(R.id.tv_appName, newname);
                                    }


                                }

                                if (isDebug) {
                                    Log.i("DB_BRAZEN", "解析到的最终文件名为：" + name);
                                }

//                                builder.setContentTitle(name);
                                File file = new File(DIR, name);
                                long total = 0L;
                                String length = conn.getHeaderField("Content-length");
                                if (!TextUtils.isEmpty(length)) {
                                    total = Long.parseLong(length);
                                }

                                FileOutputStream fos = null;

                                try {
                                    fos = new FileOutputStream(file);
                                } catch (FileNotFoundException var30) {
                                    var30.printStackTrace();
                                }

                                if (null == fos) {
                                    if (isDebug) {
                                        Log.e("DB_BRAZEN", "打开文件失败：" + file);
                                    }

                                    return;
                                }

                                try {
                                    long progress = 0L;
                                    long lastTime = 0L;
                                    byte[] b = new byte[10240];
                                    int len;
                                    while ((len = is.read(b)) != -1) {
                                        fos.write(b, 0, len);
                                        progress += (long) len;
                                        if (total != 0L && System.currentTimeMillis() - lastTime > 1000L) {
                                            int p = (int) ((double) progress * 1.0D / (double) total * 100.0D);
//                                            builder.setProgress(100, p, false);
                                            contentView.setProgressBar(R.id.pb_downloadProgress, 100, p, false);
                                            contentView.setTextViewText(R.id.tv_appProgress, p + "%");
//
                                            mBuilder.setContent(contentView);
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                                this.NMC.notify(info.id, mBuilder.build());
                                            } else {
                                                this.NMC.notify(info.id, mBuilder.getNotification());
                                            }
                                            lastTime = System.currentTimeMillis();
                                            if (isDebug) {
                                                Log.i("DB_BRAZEN", "下载进度：" + progress);
                                            }
                                        }
                                    }

                                    fos.flush();
                                    fos.close();
                                    is.close();
                                    if (isDebug) {
                                        Log.i("DB_BRAZEN", "下载完成：" + file);
                                    }

                                    this.NMC.cancel(info.id);
                                    if (null != info.rpFinish) {
//                                        Api.common(this).report(info.rpFinish, (String) null, (String) null);
                                        if (isDebug) {
                                            Log.d("DB_BRAZEN", "上报应用下载完成");
                                        }
                                    }

                                    this.DOWNLOADING.remove(info.link);
                                    PackageManager pm = this.getPackageManager();
                                    if (null == pm) {
                                        continue;
                                    }

                                    PackageInfo pi = pm.getPackageArchiveInfo(file.getAbsolutePath(), 0);
                                    if (null == pi) {
                                        continue;
                                    }
                                    Intent download = new Intent(ACTION_DOWNLOAD_FINISH);
                                    download.putExtra(NewBrazenReceiver.KEY_PACKAGE, pi.packageName);
                                    download.putExtra(NewBrazenReceiver.KEY_INFO, info);
                                    this.sendBroadcast(download);
                                    Intent install = new Intent(Intent.ACTION_VIEW);
                                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        String path = file.getAbsolutePath();
                                        Log.d("DB_BRAZEN","路径>>>"+path);
                                        this.getPackageName()
                                        chmod("777",file.getAbsolutePath());
                                        Uri apkUri = FileProvider.getUriForFile(this, this.getPackageName() + ".fileprovider", file);
                                        install.setDataAndType(apkUri, "application/vnd.android.package-archive");
                                        //兼容8.0
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            boolean hasInstallPermission = this.getPackageManager().canRequestPackageInstalls();
                                            if (!hasInstallPermission) {
                                                Toast.makeText(this, "不能安装未知来源的应用", Toast.LENGTH_SHORT).show();
                                                startInstallPermissionSettingActivity();
                                                return;
                                            }
                                        }
                                    } else {
                                        install.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                                    }
                                    install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    List<ResolveInfo> ri = pm.queryIntentActivities(install, 0);
                                    if (null == ri || ri.size() == 0) {
                                        continue;
                                    }
                                    this.startActivity(install);
                                } catch (IOException var35) {
                                    this.NMC.cancel(info.id);
                                    var35.printStackTrace();
                                }
                                break;
                            case 3:
                                if (isDebug) {
                                    Log.d("DB_BRAZEN", "链接地址需要重定向");
                                }

                                String location = conn.getHeaderField("location");
                                if (TextUtils.isEmpty(location)) {
                                    if (isDebug) {
                                        Log.e("DB_BRAZEN", "不能从location参数中获取重定向地址");
                                    }

                                    return;
                                }

                                if (isDebug) {
                                    Log.i("DB_BRAZEN", "得到新的重定向地址：" + location);
                                }

                                redirect = location;
                                ++count;
                                continue;
                            case 4:
                                if (isDebug) {
                                    Log.e("DB_BRAZEN", "客户端错误：" + status);
                                }
                                break;
                            case 5:
                                if (isDebug) {
                                    Log.e("DB_BRAZEN", "服务器错误：" + status);
                                }
                                break;
                            default:
                                continue;
                        }
                    }

                    return;
                }
            }
        }
    }

    /**
     * 跳转到设置-允许安装未知来源-页面
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startInstallPermissionSettingActivity() {
        //注意这个是8.0新API
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    private void downLoadPic(String iconurl, final RemoteViews contentView) {
        if (TextUtils.isEmpty(iconurl)) {
            return;
        }
        ImageSize imagesize = new ImageSize(dip2px(this, 48), dip2px(this, 48));
        ImageLoader.getInstance().loadImage(iconurl, imagesize, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
                System.out.print("sss");
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                System.out.print("sss");
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                System.out.print("sss");
                contentView.setImageViewBitmap(R.id.iv_appIcon, loadedImage);
            }


            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                System.out.print("sss");
            }
        });
 /*        InputStream is = null;
      if (!TextUtils.isEmpty(iconurl)) {
            try {
                URL iconlink = new URL(iconurl);
                HttpURLConnection iconURLConnection = buildConnWithHttp(iconlink);
                iconURLConnection.setReadTimeout(30 * 1000);
                iconURLConnection.setConnectTimeout(30 * 1000);
                iconURLConnection.setDoInput(true);
                iconURLConnection.setDoOutput(true);
                iconURLConnection.connect();
                if(iconURLConnection.getResponseCode()==200){
                    is = iconURLConnection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    if (null != bitmap) {
                        contentView.setImageViewBitmap(R.id.iv_appIcon, bitmap);
                    }
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != is) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }*/
    }

    private String buildFileName(CustomDownloadInfo info, HttpURLConnection conn) {
        String name = null;
        if (!TextUtils.isEmpty(info.title)) {
            name = info.title + ".apk";
        }

        String url;
        if (TextUtils.isEmpty(name)) {
            url = conn.getHeaderField("Content-Disposition");
            if (!TextUtils.isEmpty(url)) {
                if (isDebug) {
                    Log.e("DB_BRAZEN", "获取到Content-Disposition：" + url);
                }

                name = url.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
                if (isDebug) {
                    Log.i("DB_BRAZEN", "从Content-Disposition解析到文件名：" + name);
                }

                try {
                    name = URLDecoder.decode(name, "ISO-8859-1");
                    if (isDebug) {
                        Log.i("DB_BRAZEN", "通过URL Decode解码到文件名：" + name);
                    }
                } catch (UnsupportedEncodingException var8) {
                    var8.printStackTrace();
                }
            }
        }

        if (TextUtils.isEmpty(name)) {
            if (isDebug) {
                Log.d("DB_BRAZEN", "尝试以腾讯方式解析文件名");
            }

            Uri uri = Uri.parse(info.link);
            name = uri.getQueryParameter("fsname");
        }

        if (TextUtils.isEmpty(name)) {
            if (isDebug) {
                Log.d("DB_BRAZEN", "尝试以360方式解析文件名");
            }

            url = info.link;

            try {
                url = URLDecoder.decode(url, "utf8");
            } catch (UnsupportedEncodingException var7) {
                var7.printStackTrace();
            }

            Uri uri = Uri.parse(url);
            url = uri.getQueryParameter("url");
            if (null != url && (url.startsWith("http") || url.startsWith("https")) && url.endsWith(".apk")) {
                String[] paths = url.split("/");
                if (paths.length != 0) {
                    name = paths[paths.length - 1];
                }
            }
        }

        if (TextUtils.isEmpty(name)) {
            name = UIUtils.md5(info.link, false, false) + ".apk";
        }

        return name;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(String channeId, String channeIname, int importance) {
        NotificationChannel channel = new NotificationChannel(channeId, channeIname, importance);
        channel.enableLights(true); //是否在桌面icon右上角展示小红点
        channel.setLightColor(Color.GREEN); //小红点颜色
        channel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
        NMC.createNotificationChannel(channel);
    }

    private HttpURLConnection buildConnWithHttp(URL link) {
        HttpURLConnection conn = null;

        try {
            conn = (HttpURLConnection) link.openConnection();
        } catch (IOException var4) {
            var4.printStackTrace();
        }

        return conn;
    }
    public void chmod(String permission, String path) {
        try {
            String command = "chmod " + permission + " " + path;
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private HttpURLConnection buildConnWithHttps(URL link) {
        HttpsURLConnection conn = null;

        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init((KeyManager[]) null, new TrustManager[]{this.TRUST}, new SecureRandom());
            conn = (HttpsURLConnection) link.openConnection();
            conn.setSSLSocketFactory(ctx.getSocketFactory());
            conn.setRequestMethod("GET");
        } catch (NoSuchAlgorithmException | KeyManagementException | IOException var4) {
            var4.printStackTrace();
        }

        return conn;
    }

    static {
        DIR = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Brazen");
    }

    public static class CustomDownloadInfo implements Serializable {
        int id;
        String link;
        String title;
        String[] rpStart;
        String[] rpFinish;
        String[] rpInstall;
        String[] rpActivate;
        String iconurl;


        public CustomDownloadInfo(String link, String[] rpStart, String[] rpFinish, String[] rpInstall, String[] rpActivate) {
            this.id = (new Random()).nextInt();
            this.link = link;
            this.rpStart = rpStart;
            this.rpFinish = rpFinish;
            this.rpInstall = rpInstall;
            this.rpActivate = rpActivate;
        }

        public CustomDownloadInfo(String link, String title, String iconurl, String[] rpStart, String[] rpFinish, String[] rpInstall, String[] rpActivate) {
            this.id = (new Random()).nextInt();
            this.link = link;
            this.iconurl = iconurl;
            this.title = title;
            this.rpStart = rpStart;
            this.rpFinish = rpFinish;
            this.rpInstall = rpInstall;
            this.rpActivate = rpActivate;
        }
    }


}
