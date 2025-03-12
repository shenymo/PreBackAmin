package com.xeasy.prebackanim.hook;

import static com.xeasy.prebackanim.dao.BlackListDao.blackList4AppHashMap;
import static com.xeasy.prebackanim.dao.BlackListDao.gson;

import android.annotation.SuppressLint;
import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.util.Log;

import com.google.gson.reflect.TypeToken;
import com.xeasy.prebackanim.dao.BlackListDao;
import com.xeasy.prebackanim.utils.ReflectUtils2;
import com.xeasy.prebackanim.utils.ReflexUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookImpl implements IXposedHookLoadPackage {

    public static final String LOG_PREV = "Prebackanimation---";
    public static final boolean IS_LOG = true;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedBridge.log("启动的应用: " + loadPackageParam.packageName);


// 返回动画FLAG com.xeasy.noticefix.activity.MainActivity.activeXposed
        // 针对所有应用
        XposedHelpers.findAndHookConstructor(
                "android.content.pm.ApplicationInfo",
                loadPackageParam.classLoader,
                "android.content.pm.ApplicationInfo",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
//                        if ( IS_LOG ) XposedBridge.log(LOG_PREV + "###   运行了运行了!!!!!  ########! ");
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            return;
                        }
                        ApplicationInfo appInfo = (ApplicationInfo) param.args[0];

                        try {
                            // 修改flags
                            @SuppressLint("BlockedPrivateApi") Field flagsField = ApplicationInfo.class.getDeclaredField("privateFlagsExt");
//                            if ( IS_LOG ) XposedBridge.log(LOG_PREV + "flagsField = " +flagsField);
                            flagsField.setAccessible(true);
                            int flags = flagsField.getInt(appInfo);
//                            if ( IS_LOG ) XposedBridge.log(LOG_PREV + "flagsField = " +flagsField);
//                            if ( IS_LOG ) XposedBridge.log(LOG_PREV + "pkg = " +appInfo.packageName+ "flags --- " + flags);
                            if ((flags & (1 << 3)) != 0) {
//                                if ( IS_LOG ) XposedBridge.log(LOG_PREV + "pkg = " +appInfo.packageName+ " 包含!!!");
                            } else {
//                                if ( IS_LOG ) XposedBridge.log(LOG_PREV + "pkg = " +appInfo.packageName+ " 没有 !!!");
                                flags |= 1 << 3;
                                flagsField.setInt(appInfo, flags);
                            }

                            // 可选：修改targetSdkVersion绕过检查 此行代码会触发微信无限重启主Activity 耗电非常严重
//                            Field targetSdkField = ApplicationInfo.class.getDeclaredField("targetSdkVersion");
//                            targetSdkField.setAccessible(true);
//                            targetSdkField.setInt(appInfo, Build.VERSION_CODES.TIRAMISU);
                        } catch (Exception e) {
                            if (IS_LOG) XposedBridge.log(e);
                            if (IS_LOG) XposedBridge.log("# 出错了!");
                        }
                    }
                }
        );

        // 针对所有应用
        XposedHelpers.findAndHookConstructor(
                "android.content.pm.ApplicationInfo",
                loadPackageParam.classLoader,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
//                        if ( IS_LOG ) XposedBridge.log(LOG_PREV + "###   运行了运行了!!!!!  ########! ");
//                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
//                            return;
//                        }
                        ApplicationInfo appInfo = (ApplicationInfo) param.thisObject;

                        try {
                            // 修改flags
                            @SuppressLint("BlockedPrivateApi") Field flagsField = ApplicationInfo.class.getDeclaredField("privateFlagsExt");
//                            if ( IS_LOG ) XposedBridge.log(LOG_PREV + "flagsField = " +flagsField);
                            flagsField.setAccessible(true);
                            int flags = flagsField.getInt(appInfo);
                            flags |= 1 << 3;
                            flagsField.setInt(appInfo, flags);
                            // 可选：修改targetSdkVersion绕过检查 此行代码会触发微信无限重启主Activity 耗电非常严重
//                            Field targetSdkField = ApplicationInfo.class.getDeclaredField("targetSdkVersion");
//                            targetSdkField.setAccessible(true);
//                            targetSdkField.setInt(appInfo, Build.VERSION_CODES.TIRAMISU);
                        } catch (Exception e) {
                            if (IS_LOG) XposedBridge.log(e);
                            if (IS_LOG) XposedBridge.log("# 出错了!");
                        }
                    }
                }
        );


//        if ( loadPackageParam.packageName.equals("com.tencent.mm")) {
//            return;
//        }

        Class<?> aClass = XposedHelpers.findClass("android.app.ActivityThread", loadPackageParam.classLoader);
        Method handleLaunchActivity = ReflexUtil.findMethodIfParamExist(aClass, "handleLaunchActivity");
        if ( IS_LOG) XposedBridge.log("handleLaunchActivity" + handleLaunchActivity);
        if ( handleLaunchActivity != null ) {
            XposedBridge.hookMethod(handleLaunchActivity,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
//                            ActivityInfo thisObject = (ActivityInfo) param.thisObject;

                                // 获取 ActivityClientRecord
                                Object activityRecord = param.args[0];
                                for (Object arg : param.args) {
                                    if ( null != arg && arg.getClass().getName().contains("ActivityClientRecord")) {
                                        activityRecord = arg;
                                    }
                                }
                                if ( activityRecord == null ) {
                                    if ( IS_LOG) XposedBridge.log("activityRecord 是null");
                                    return;
                                }
                                // 获取 ActivityInfo 对象
                                ActivityInfo thisObject = (ActivityInfo) XposedHelpers.getObjectField(activityRecord, "activityInfo");

                                XposedBridge.log("####  启动了activity " + thisObject.name);

                                if (IS_LOG) {

                                    XposedBridge.log("#### Parcel afterHookedMethod 启动了activity packageName " + thisObject.packageName);
                                }
                                if (IS_LOG)
                                    XposedBridge.log("#### Parcel afterHookedMethod 启动了activity privateFlags " + XposedHelpers.getIntField(thisObject, "privateFlags"));
                                try {
                                    // 修改flags
                                    @SuppressLint("BlockedPrivateApi") Field flagsField = ActivityInfo.class.getDeclaredField("privateFlags");
//                            if ( IS_LOG ) XposedBridge.log(LOG_PREV + "flagsField = " +flagsField);
                                    flagsField.setAccessible(true);
                                    int flags = flagsField.getInt(thisObject);

                                    Class<?> aClass = XposedHelpers.findClassIfExists(thisObject.name, loadPackageParam.classLoader);
                                    ;
                                    if (aClass == null) {
                                        return;
                                    }
                                    Log.d("name ", AndroidAppHelper.currentApplication().getPackageName());
                                    if (!BlackListDao.inited && !AndroidAppHelper.currentApplication().getPackageName().equals("com.xeasy.prebackanim")) {
                                        readConfig(AndroidAppHelper.currentApplication());
                                    }

                                    if (BlackListDao.inited
                                            && blackList4AppHashMap.containsKey(thisObject.packageName)
                                            && blackList4AppHashMap.get(thisObject.packageName).activityNameMap.containsKey(thisObject.name)
                                            && blackList4AppHashMap.get(thisObject.packageName).activityNameMap.get(thisObject.name)
                                    ) {
                                        // 判断该activity是否重写了 onBackPressed 是的话 标记为不支持预返回动画
                                        if (IS_LOG) XposedBridge.log("在黑名单");
                                        flags |= 1 << 3;
                                        flags &= ~(1 << 2);
                                    } else {
                                        if (IS_LOG) XposedBridge.log("不在黑名单");
                                        flags |= 1 << 2;
                                        flags &= ~(1 << 3);
                                    }
                                    flagsField.setInt(thisObject, flags);
                                } catch (Exception e) {
                                    if (IS_LOG) XposedBridge.log(e);
                                    if (IS_LOG) XposedBridge.log("# 出错了!");
                                }
                            } catch (Exception e) {
                                if (IS_LOG) XposedBridge.log(e);
                            }

                        }
                    });
        }


        if (!loadPackageParam.packageName.equals("com.xeasy.prebackanim")) {
            XposedHelpers.findAndHookMethod(
                    "android.app.Application",
                    loadPackageParam.classLoader,
                    "onCreate",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Context context = (Context) param.thisObject;
                            IntentFilter filter = new IntentFilter("com.xeasy.prebackanim.UPDATE_CONFIG");
                            BroadcastReceiver receiver = new BroadcastReceiver() {
                                @Override
                                public void onReceive(Context contextinner, Intent intent) {
                                    Log.d(HookImpl.class.getName(), "#### 接收到热更新配置的信息信息!!  ");
                                    reloadConfig(contextinner);
                                }
                            };
                            try {
                                context.registerReceiver(receiver, filter);
                            } catch (Exception e) {
//                                Log.e(LOG_PREV, e.getMessage(), e);
                                Log.d(LOG_PREV, "不能重复注册接收器");
                            }
                        }
                    }
            );
        }


    }

    private void reloadConfig(Context context) {
//        BlackListDao.initConfig(context);
        readConfig(context);
        if (IS_LOG) XposedBridge.log("更新配置文件完成! " + gson.toJson(blackList4AppHashMap));
    }

    private void readConfig(Context context) {
        // Hook 代码中
        XSharedPreferences prefs = new XSharedPreferences("com.xeasy.prebackanim", "BlackListDao");
        prefs.makeWorldReadable(); // 调用内部方法强制设置可读（需系统权限）
        prefs.reload();

        String config = prefs.getString("config", "{}");
        Type type = new TypeToken<HashMap<String, BlackListDao.BlackList4App>>(){}.getType();
        blackList4AppHashMap = ( gson.fromJson(config, type) );
        BlackListDao.inited = true;
        if (IS_LOG) XposedBridge.log("读取配置文件完成! " + config);


    }

}
