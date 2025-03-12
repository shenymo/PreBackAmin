package com.xeasy.prebackanim.dao;

import static com.topjohnwu.superuser.internal.UiThreadHandler.handler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xeasy.prebackanim.utils.CommandUtil;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 预返回动画黑名单 凡是在黑名单的activity皆不执行预返回动画
 */
public class BlackListDao {

    public static Map<String, BlackList4App> blackList4AppHashMap = new HashMap<>();

    public static boolean inited;

    public static Long configVer = 1L;

    public static Gson gson = new Gson();

    public static void initConfig(Context context) {
        // 用户的应用中更新配置
//        SharedPreferences prefs = context.getSharedPreferences("BlackListDao", Context.MODE_PRIVATE);
        SharedPreferences prefs = context.getSharedPreferences("BlackListDao", Context.MODE_WORLD_READABLE);
        String config = prefs.getString("config", "{}");
        Type type = new TypeToken<HashMap<String, BlackList4App>>(){}.getType();
        blackList4AppHashMap = ( gson.fromJson(config, type) );
        inited = true;

    }

    public static Map<String, BlackList4App> getConfig(Context context) {
        if ( ! inited ) {
            initConfig(context);
        }
        return blackList4AppHashMap;
    }

    public static void saveConfig(Context context) {
        // 用户的应用中更新配置
        SharedPreferences prefs = context.getSharedPreferences("BlackListDao", Context.MODE_WORLD_READABLE);
//        SharedPreferences prefs = context.getSharedPreferences("BlackListDao", Context.MODE_PRIVATE);
        String config = gson.toJson(blackList4AppHashMap);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("config", config);
        boolean commit = edit.commit();
        if ( commit ) {
            Log.d(BlackListDao.class.getName(), "保存配置信息成功");
            // 发送更新配置的广播
            configVer ++;
            Intent intent = new Intent("com.xeasy.prebackanim.UPDATE_CONFIG");
//            intent.setPackage("com.xeasy.prebackanim"); // 显式指定包名，确保广播定向发送
            context.sendBroadcast(intent);
            /*String content = CommandUtil.execShell("dumpsys activity activities | grep \"topResumedActivity\"", true);
            handler.postDelayed(() -> {
                // 发送后 100ms 执行重启最上层activity的命令 有bug 不执行把
                Log.d("dayin","am start -n "+content.trim().split(" ")[2]+" --activity-clear-task --activity-reorder-to-front");
                CommandUtil.execShell(
                        "am start -n "+content.trim().split(" ")[2]+" --activity-clear-task --activity-reorder-to-front"
                        ,true);
            }, 100);*/

        }

    }

    public static void add2BlackList(Context context, String pkgName, String activityName ) {
        if ( !inited) {
            initConfig(context);
        }
        BlackList4App blackList4App = blackList4AppHashMap.get(pkgName);
        if ( blackList4App == null ) {
            blackList4App = new BlackList4App();
            blackList4App.pkgName = pkgName;
            blackList4App.activityNameMap = new HashMap<>();
        }
        blackList4App.activityNameMap.put(activityName, true);
        blackList4AppHashMap.put(pkgName, blackList4App);
        saveConfig(context);
    }

    public static void remove2BlackList(Context context, String pkgName, String activityName ) {
        if ( !inited) {
            initConfig(context);
        }
        BlackList4App blackList4App = blackList4AppHashMap.get(pkgName);
        if ( blackList4App == null ) {
            blackList4App = new BlackList4App();
            blackList4App.pkgName = pkgName;
            blackList4App.activityNameMap = new HashMap<>();
        }
        blackList4App.activityNameMap.remove(activityName);
        blackList4AppHashMap.put(pkgName, blackList4App);
        saveConfig(context);
    }

    public static class BlackList4App {
        /**
         * 包名
         */
        public String pkgName;
        /**
         * key: activity类名
         * value: true 黑名单 false 白名单
         */
        public HashMap<String, Boolean> activityNameMap = new HashMap<>();

    }

}
