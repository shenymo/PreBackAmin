package com.xeasy.prebackanim;

import static com.topjohnwu.superuser.internal.Utils.context;
import static com.xeasy.prebackanim.dao.BlackListDao.blackList4AppHashMap;
import static com.xeasy.prebackanim.dao.BlackListDao.gson;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.reflect.TypeToken;
import com.xeasy.prebackanim.dao.BlackListDao;
import com.xeasy.prebackanim.databinding.ActivityMainBinding;
import com.xeasy.prebackanim.service.QSTileService;
import com.xeasy.prebackanim.service.UpdateService;
import com.xeasy.prebackanim.ui.home.HomeFragment;
import com.xeasy.prebackanim.utils.CommandUtil;
import com.xeasy.prebackanim.utils.FileUtils;
import com.xeasy.prebackanim.utils.GetFilePathFromUri;
import com.xeasy.prebackanim.utils.PermissionsUtil;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private BroadcastReceiver mReceiver;


    ActivityResultLauncher<Intent> intentActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        BlackListDao.initConfig(getApplicationContext());


        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // 请求悬浮窗权限
        View btnShow = findViewById(R.id.fab);
        btnShow.setOnClickListener(v -> checkPermission());

        // 注册广播接收器
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("ACTION_TILE_CLICKED".equals(intent.getAction())) {
                    // 执行 MainActivity 中的方法
                    checkPermission();
                }
                if ("ACTION_TILE_CLOSED".equals(intent.getAction())) {
                    // 执行 MainActivity 中的方法
                    closeFloatingWindow();
                }
            }
        };

        IntentFilter filter = new IntentFilter("ACTION_TILE_CLICKED");
        registerReceiver(mReceiver, filter);


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.import_config) {
            // todo 导入
            FileUtils.openFilePicker(this, PICK_FILE_REQUEST);

        }
        if (id == R.id.export_config) {
            // todo 导出
            String config = gson.toJson(BlackListDao.blackList4AppHashMap);
            // 调用导出方法
            boolean isSuccess = FileUtils.exportToPublicDownload(this, config, "PreBackAnim");
            if (isSuccess) {
                // 导出成功
                Toast.makeText(this, "文件已保存至Download目录", Toast.LENGTH_SHORT).show();
            } else {
                // 导出失败
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
            }
        }
        if (id == R.id.wiki) {
            // todo 打开哔哩哔哩个人主页
            String url = "https://space.bilibili.com/261717574";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "未找到浏览器应用", Toast.LENGTH_SHORT).show();
            }

        }

        return super.onOptionsItemSelected(item);
    }

    private static final int PICK_FILE_REQUEST = 1;


    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeFloatingWindow();
        unregisterReceiver(mReceiver); // 解注册
    }

    private static final int OVERLAY_PERMISSION_CODE = 1001;

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
        } else {
            showFloatingWindow();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                showFloatingWindow();
            }
        }
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String content = FileUtils.readTextFile(this, uri);
            if (content != null) {

                Type type = new TypeToken<HashMap<String, BlackListDao.BlackList4App>>() {
                }.getType();
                blackList4AppHashMap = (gson.fromJson(content, type));
                // 成功读取文件内容
                Toast.makeText(this, "导入成功 App数量：" + blackList4AppHashMap.size(), Toast.LENGTH_SHORT).show();
                BlackListDao.saveConfig(this);

                launchFragment(new HomeFragment());

            } else {
                Toast.makeText(this, "读取失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.nav_host_fragment_content_main, fragment);
        transaction.addToBackStack(null); // 可选：将事务添加到返回栈
        transaction.commit();
    }

    WindowManager windowManager = null;
    View floatingView = null;

    // 定义磁贴服务的 ComponentName
    private ComponentName mTileComponent;

    private void showFloatingWindow() {
        if (floatingView != null) {
            return;
        }
        // 请求root和浮窗权限
        CommandUtil.execShell("su", true);


        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.floating_window, null);

        // 初始化磁贴服务的 ComponentName
        mTileComponent = new ComponentName(this, QSTileService.class);
        // 设置按钮点击事件
        Button btnClose = floatingView.findViewById(R.id.btn_close);

        TextView pkgName = floatingView.findViewById(R.id.pkgName);
        TextView activityName = floatingView.findViewById(R.id.activityName);

        btnClose.setOnClickListener(v -> closeFloatingWindow());
        // todo 处理黑名单添加逻辑
        Button add2blackListBtn = floatingView.findViewById(R.id.add2blackList);
        add2blackListBtn.setOnClickListener(v -> {
//            Toast.makeText(this, "已添加黑名单 !", Toast.LENGTH_SHORT).show();
            PermissionsUtil.reqPermission(this, Manifest.permission.POST_NOTIFICATIONS, () -> {
                BlackListDao.add2BlackList(this, (String) pkgName.getText(), (String) activityName.getText());
//                GlobalToast.show(getApplicationContext(), "已添加黑名单 !");
                Toast.makeText(this, "已添加黑名单 !", Toast.LENGTH_SHORT).show();
                return null;
            });

        });
        // todo 处理黑名单添加逻辑
        Button remove2BlackList = floatingView.findViewById(R.id.remove2blackList);
        remove2BlackList.setOnClickListener(v -> {
//            Toast.makeText(this, "已添加黑名单 !", Toast.LENGTH_SHORT).show();
            PermissionsUtil.reqPermission(this, Manifest.permission.POST_NOTIFICATIONS, () -> {
                BlackListDao.remove2BlackList(this, (String) pkgName.getText(), (String) activityName.getText());
//                GlobalToast.show(getApplicationContext(), "已添加黑名单 !");
                Toast.makeText(this, "已移出黑名单 !", Toast.LENGTH_SHORT).show();
                return null;
            });

        });


        // 设置窗口参数
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        // 添加触摸移动支持
        View rootView = floatingView.findViewById(R.id.root_layout);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingView, params);


        ImageView iconView = floatingView.findViewById(R.id.app_info_icon);
        TextView nameView = floatingView.findViewById(R.id.app_info_name);
        View floatView = floatingView.findViewById(R.id.root_layout);
//        TextView tvText = floatingView.findViewById(R.id.activityName);
//        TextView pkgName = floatingView.findViewById(R.id.pkgName);

        // 绑定服务
        Intent serviceIntent = new Intent(this, UpdateService.class);
        startService(serviceIntent);
        bindService(serviceIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                UpdateService.Binder binder = (UpdateService.Binder) service;
                UpdateService updateService = binder.getService();
                updateService.setTextView(floatView);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        }, BIND_AUTO_CREATE);
    }

    private void closeFloatingWindow() {
        if (floatingView != null && floatingView.isAttachedToWindow()) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }

        // 停止服务
        Intent serviceIntent = new Intent(this, UpdateService.class);
        stopService(serviceIntent);

        // 1. 保存新状态到 SharedPreferences
        SharedPreferences prefs = getSharedPreferences("tile_config", MODE_PRIVATE);
        prefs.edit().putBoolean("is_active", false).apply();

        // 2. 请求磁贴刷新
        TileService.requestListeningState(this, mTileComponent);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}