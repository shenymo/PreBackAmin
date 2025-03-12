package com.xeasy.prebackanim.service;

import static com.topjohnwu.superuser.internal.UiThreadHandler.handler;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import com.xeasy.prebackanim.MainActivity;
import com.xeasy.prebackanim.R;

public class QSTileService extends TileService {

    private static final String TAG = "QuickSettingsTile";

    @Override
    public void onClick() {
        super.onClick();
        Tile tile = getQsTile(); // 获取磁贴对象
        int currentState = tile.getState();

        // 切换状态
        switch (currentState) {
            case Tile.STATE_INACTIVE:
                tile.setState(Tile.STATE_ACTIVE);
                // 执行激活操作（例如开启服务）

                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("openFloatWindow", true); // 附加参数
                startActivity(intent);

                handler.postDelayed(() -> {
                    // 发送广播
                    Intent intent3 = new Intent("ACTION_TILE_CLICKED");
                    sendBroadcast(intent3);
                }, 100);

                break;
            case Tile.STATE_ACTIVE:
                tile.setState(Tile.STATE_INACTIVE);
                // 执行关闭操作
                // 发送广播
                Intent intent2 = new Intent("ACTION_TILE_CLOSED");
                sendBroadcast(intent2);
                break;
        }

        // 更新图标和标签
        tile.setIcon(Icon.createWithResource(this, R.mipmap.img));
//        tile.setLabel(getString(R.string.tile_label));
        tile.updateTile(); // 必须调用以刷新界面
    }

    // 其他生命周期方法
    @Override
    public void onTileAdded() {
        super.onTileAdded();
        Log.d(TAG, "onTileAdded: 用户添加了磁贴");
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        Log.d(TAG, "onTileRemoved: 用户移除了磁贴");
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileFromPrefs();
    }

    private void updateTileFromPrefs() {
        Tile tile = getQsTile();
        if (tile == null) return;

        // 从 SharedPreferences 读取状态
        SharedPreferences prefs = getSharedPreferences("tile_config", MODE_PRIVATE);
        boolean isActive = prefs.getBoolean("is_active", false);

        // 更新磁贴状态和图标
        tile.setState(isActive ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
//        tile.setIcon(Icon.createWithResource(this,
//                isActive ? R.drawable.ic_tile_active : R.drawable.ic_tile_inactive));
//        tile.setLabel(getString(isActive ? R.string.tile_active : R.string.tile_inactive));

        tile.updateTile(); // 必须调用
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        // 面板关闭时释放资源
    }

}
