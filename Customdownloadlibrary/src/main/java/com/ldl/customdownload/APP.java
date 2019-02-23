package com.ldl.customdownload;

import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.util.DisplayMetrics;

import com.nostra13.universalimageloader.cache.disc.impl.LimitedAgeDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.util.concurrent.Executors;

public class APP extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initImageLoader(this);
        initReceiver();

    }

    private void initImageLoader(Context context) {
        // TODO Auto-generated method stub
        // 创建DisplayImageOptions对象
        DisplayImageOptions defaulOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true).cacheOnDisk(true).build();
        // 创建ImageLoaderConfiguration对象
        ImageLoaderConfiguration configuration = new ImageLoaderConfiguration.Builder(
                context).defaultDisplayImageOptions(defaulOptions)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                .tasksProcessingOrder(QueueProcessingType.LIFO).build();
        // ImageLoader对象的配置
        ImageLoader.getInstance().init(configuration);
    }

    private void initReceiver() {
        IntentFilter inputFilter = new IntentFilter();
        inputFilter.addDataScheme("package");
        inputFilter.addAction("android.intent.action.PACKAGE_ADDED");
        inputFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        IntentFilter inputFilter2 = new IntentFilter();
        inputFilter2.addAction("com.sant.brazen.action.ACTION_DOWNLOAD_FINISH");
        NewBrazenReceiver newbrazenreceiver = new NewBrazenReceiver();
        this.registerReceiver(newbrazenreceiver, inputFilter);
        this.registerReceiver(newbrazenreceiver, inputFilter2);
    }

}
