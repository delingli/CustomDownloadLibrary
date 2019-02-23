package com.ldl.test;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.ldl.customdownload.CustomDownLoadService;

public class MainActivity extends AppCompatActivity {
    private static int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    String url = "https://oss.ucdl.pp.uc.cn/fs01/union_pack/Wandoujia_628885_web_seo_baidu_binded.apk?x-oss-process=udf%2Fpp-udf%2CJjc3LiMnJ3FycXFycHM%3D";
    String pic_url = "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1550642291020&di=a157d9c9256733686237c1b7b4ddc327&imgtype=0&src=http%3A%2F%2Fimg.mp.itc.cn%2Fupload%2F20170721%2F2acebd71c5254bacb890fa37febd26d0_th.jpg";

    public void btnDownload(View v) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                如果应用之前请求过次权限但用户拒绝了
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
            } else {
//                如果用户不仅拒绝上次的请求权限，而且勾选了“不再提示”
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
            }

        }else{
            toStartService();
        }

    }

    private void toStartService() {
        CustomDownLoadService.CustomDownloadInfo info = new CustomDownLoadService.CustomDownloadInfo(url, "", pic_url, null, null, null, null);
        Intent intents = new Intent(MainActivity.this, CustomDownLoadService.class);
        intents.putExtra(CustomDownLoadService.EXTRA_INFO, info);
        startService(intents);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {//授权
                toStartService();

            } else {//拒绝
                Toast.makeText(MainActivity.this,"拒绝就没法用哦!",Toast.LENGTH_SHORT).show();
            }
            return;

        }


    }
}
