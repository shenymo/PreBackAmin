package com.xeasy.prebackanim.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.FutureTask;

public class AppUtil {

    public static Map<Integer, Integer> appCount;

    private static List<AppInfo4View> appInfo4ViewList;
    public static final Map<String, AppInfo4View> appInfo4ViewMap = new HashMap<>();

    public static FutureTask<List<AppInfo4View>> cacheTask = null;


    /**
     * 获取手机已安装应用列表
     *
     * @param ctx c
     * @return list
     */
    public static List<PackageInfo> getAllAppInfo(Context ctx) {
        PackageManager packageManager = ctx.getPackageManager();
        return packageManager.getInstalledPackages(0);
    }

    /**
     * 获取手机已安装应用列表
     *
     * @param ctx c
     * @return list
     */
    public static PackageInfo getAppInfo(Context ctx, String packageName) {
        try {
            PackageManager packageManager = ctx.getPackageManager();
            return packageManager.getPackageInfo(packageName, 0);
        } catch ( Exception e) {
            return null;
        }
    }

    public static AppInfo4View getApp4ViewByPackageInfo(Context context, PackageInfo packageInfo) {
        PackageManager packageManager = context.getPackageManager();
        AppInfo4View cache = appInfo4ViewMap.get(packageInfo.applicationInfo.packageName);
        if ( cache != null ) {
            return cache;
        }
//        System.out.println("cache没有命中=" + packageInfo.applicationInfo.packageName);
        AppInfo4View appInfo4View = new AppInfo4View();
        appInfo4View.AppName = (packageManager.getApplicationLabel(packageInfo.applicationInfo).toString());
        appInfo4View.AppPkg = (packageInfo.applicationInfo.packageName);
        appInfo4View.version = (packageInfo.versionName);
        int flags = packageInfo.applicationInfo.flags;
        appInfo4View.isSystem = (flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        appInfo4View.versionAndType = appInfo4View.isSystem ?
                "isSystemApp"
                : "isUserApp";
        appInfo4View.AppIcon = (packageInfo.applicationInfo.loadIcon(packageManager));
        // todo
        appInfo4View.lastIcon = null;
        // 匹配 lib_icon
        appInfo4ViewMap.put(appInfo4View.AppPkg, appInfo4View);
        return appInfo4View;
    }
    public static AppInfo4View getApp4ViewByPackageName(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            return getApp4ViewByPackageInfo(context, packageInfo);
        } catch (Exception e) {
            return null;
        }
    }

}
