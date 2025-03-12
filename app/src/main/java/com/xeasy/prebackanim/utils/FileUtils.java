package com.xeasy.prebackanim.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Callable;

public class FileUtils {


    public static void saveString(Context context, String str, String fileName) {

        Callable<Objects> callable = () -> {
            try {
                String format = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date());
                String filePath = Environment.getExternalStorageDirectory().getPath() + "/Download/" + fileName + format + ".json";
                Uri saveUri = new Uri.Builder().path(filePath).build();
                OutputStream outputStream = context.getContentResolver().openOutputStream(saveUri);
                assert outputStream != null;
                outputStream.write(str.getBytes());
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "导出失败! ", Toast.LENGTH_SHORT).show();

            }
            return null;
        };
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            PermissionsUtil.reqPermission((Activity) context, Manifest.permission.WRITE_EXTERNAL_STORAGE, callable);
        } else {
            PermissionsUtil.reqPermission((Activity) context, Manifest.permission.WRITE_EXTERNAL_STORAGE, callable);
        }
    }


    public static boolean exportToInternalDownload(Context context, String content, String fileName) {
        // 获取应用私有Download目录
        File downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

        if (downloadDir == null) {
            return false;
        }

        File file = new File(downloadDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static boolean exportToPublicDownload(Context context, String content, String fileName) {
        String format = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date());
        // 检查权限和API版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上需使用MediaStore
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName + "--" + format);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream os = resolver.openOutputStream(uri)) {
                    os.write(content.getBytes());
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return false;
        } else {
            // Android 9及以下使用传统方法
            File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!publicDir.exists()) publicDir.mkdirs();

            File file = new File(publicDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(content.getBytes());
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }


    // 打开文件选择器，默认定位到公共 Download 目录
    public static void openFilePicker(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain"); // 限定选择文本文件

        // 尝试设置初始目录为公共 Download（仅限 Android 7+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri downloadUri = Uri.parse(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, downloadUri);
        }

        activity.startActivityForResult(intent, requestCode);
    }

    // 处理选择的文件
    public static String readTextFile(Activity activity, Uri uri) {
        try (java.io.InputStream inputStream = activity.getContentResolver().openInputStream(uri)) {
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            return new String(buffer);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
