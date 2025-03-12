package com.xeasy.prebackanim.utils;

import android.content.Context;
import android.os.Build;
import android.view.WindowManager;
import android.widget.Toast;

import java.lang.reflect.Field;

public class GlobalToast {
    private static Toast currentToast;

    public static void show(Context context, String message) {
        // 取消之前的 Toast
        if (currentToast != null) {
            currentToast.cancel();
        }

        // 使用 Application Context
        Context appContext = context.getApplicationContext();

        // 创建新 Toast
        currentToast = Toast.makeText(appContext, message, Toast.LENGTH_SHORT);

        // 适配 Android 11+ 的显示限制
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            currentToast.addCallback(new Toast.Callback() {
                @Override
                public void onToastShown() {
                    // 确保 Toast 窗口类型正确
                    try {
                        Field mTNField = currentToast.getClass().getDeclaredField("mTN");
                        mTNField.setAccessible(true);
                        Object mTN = mTNField.get(currentToast);

                        Field paramsField = mTN.getClass().getDeclaredField("mParams");
                        paramsField.setAccessible(true);
                        WindowManager.LayoutParams params = (WindowManager.LayoutParams) paramsField.get(mTN);

                        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                        } else {
                            params.type = WindowManager.LayoutParams.TYPE_TOAST;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        currentToast.show();
    }
}