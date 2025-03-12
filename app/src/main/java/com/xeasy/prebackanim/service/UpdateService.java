package com.xeasy.prebackanim.service;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.AndroidAppHelper;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.xeasy.prebackanim.R;
import com.xeasy.prebackanim.dao.BlackListDao;
import com.xeasy.prebackanim.utils.AppInfo4View;
import com.xeasy.prebackanim.utils.AppUtil;
import com.xeasy.prebackanim.utils.CommandUtil;

import java.lang.ref.WeakReference;

public class UpdateService extends Service {
    private static final int NOTIFICATION_ID = 123;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private WeakReference<View> floatView; // 使用弱引用防止内存泄漏

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        initUpdateTask();
    }

    private void startForegroundService() {
        NotificationChannel channel = new NotificationChannel(
                "update_channel",
                "数据更新服务",
                NotificationManager.IMPORTANCE_LOW
        );

//        NotificationManager manager = (NotificationManager)
//                getSystemService(NOTIFICATION_SERVICE);
//        manager.createNotificationChannel(channel);
//
//        Notification notification = new NotificationCompat.Builder(this, "update_channel")
//                .setContentTitle("debug浮窗运行中")
//                .setSmallIcon(R.drawable.ic_notification)
//                .build();
//
//        startForeground(NOTIFICATION_ID, notification);
    }

    private void initUpdateTask() {
        updateHandler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateText();
                updateHandler.postDelayed(this, 1000);
            }
        };
        updateHandler.post(updateRunnable);
    }

    private void updateText() {
        if (floatView != null && floatView.get() != null) {
            TextView actView = floatView.get().findViewById(R.id.activityName);
            TextView pkg = floatView.get().findViewById(R.id.pkgName);
            TextView name = floatView.get().findViewById(R.id.app_info_name);
            ImageView imageView = floatView.get().findViewById(R.id.app_info_icon);
//            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
//                    .format(new Date());
//            String content = "当前时间: " + time + "\n数据更新次数: " + System.currentTimeMillis();

            String content = CommandUtil.execShell("dumpsys activity activities | grep \"topResumedActivity\"", true);
            // 格式化content 示例数据 topResumedActivity=ActivityRecord{426f933 u0 com.tencent.mm/.ui.LauncherUI t2123}
            String[] split = content.trim().split(" ");
            if ( split.length > 3 && content.contains("ActivityRecord")) {
                String pkgNameAndActivity = split[2];
                String[] split1 = pkgNameAndActivity.split("/");

                pkg.setText(split1[0]);
                // 获得app名称和图标
                AppInfo4View app4ViewByPackageName = AppUtil.getApp4ViewByPackageName(this, split1[0]);
                // 设置app图标和名称
                imageView.setImageDrawable(app4ViewByPackageName.AppIcon);
                name.setText(app4ViewByPackageName.AppName);

                // split1[1] 判断是不是 . 开头, 是的话需要把包名拼接在前
                String activityName = split1[1];
                if ( activityName.startsWith(".")) activityName = split1[0] + activityName;
                actView.setText(activityName);

                // 判断是否在黑名单, 在的话, 把拉黑转为 移除黑名单
                if (BlackListDao.inited
                        && BlackListDao.blackList4AppHashMap.containsKey(split1[0])
                        && BlackListDao.blackList4AppHashMap.get(split1[0]).activityNameMap.containsKey(activityName)
                        && BlackListDao.blackList4AppHashMap.get(split1[0]).activityNameMap.get(activityName)
                ) {
                    Button button = floatView.get().findViewById(R.id.remove2blackList);
                    button.setVisibility(VISIBLE);
                    Button button2 = floatView.get().findViewById(R.id.add2blackList);
                    button2.setVisibility(GONE);
                } else {
                    Button button = floatView.get().findViewById(R.id.remove2blackList);
                    button.setVisibility(GONE);
                    Button button2 = floatView.get().findViewById(R.id.add2blackList);
                    button2.setVisibility(VISIBLE);
                }
            } else {
                actView.setText(content);
            }
        }
    }

    public void setTextView(View floatView) {
        this.floatView = new WeakReference<>(floatView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (updateHandler != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    public class Binder extends android.os.Binder {
        public UpdateService getService() {
            return UpdateService.this;
        }
    }
}