package com.xeasy.prebackanim.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.xeasy.prebackanim.dao.BlackListDao;

import java.util.Map;

public class ConfigProvider extends ContentProvider {
    public ConfigProvider() {
    }
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO: Implement this to handle requests to insert a new row.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onCreate() {
        // TODO: Implement this to initialize your content provider on startup.
        // 实现它以在启动时初始化您的内容提供程序
        return false;
    }

    static Gson gson = new Gson();
    //  content://com.xeasy.prebackanim.provider.configProvider
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // TODO: Implement this to handle query requests from clients.
        Map<String, BlackListDao.BlackList4App> config = BlackListDao.getConfig(getContext());
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"config"});
        Log.d("config", gson.toJson(config));
        matrixCursor.addRow(new Object[]{
                gson.toJson(config),
        });
        return matrixCursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}