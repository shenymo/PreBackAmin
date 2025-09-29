# Predictive Back Zygisk PoC

这个目录提供了一个最小化的 Zygisk 模块示例，用来在应用进程专有化后强制开启 `ApplicationInfo.privateFlagsExt` 的 `ENABLE_ON_BACK_INVOKED_CALLBACK` 位（即 `1 << 3`）。仅在 Android 13 及以上生效。

## 目录结构

- `native/`：C++ 源码与 CMake 构建脚本。
- `build.sh`：使用 CMake+NDK 交叉编译并将生成的 so 拷贝到 `module/zygisk/<abi>/`。
- `module/`：Magisk 模块打包所需文件，`module.prop` 及待放置的 Zygisk so。

## 构建步骤

1. 安装 Android NDK，并将 `ANDROID_NDK_HOME`（或 `ANDROID_NDK`）环境变量指向 NDK 路径。
2. 进入 `zygisk-preback` 目录，执行：
   ```sh
   ./build.sh
   ```
   默认会构建 `arm64-v8a` 与 `armeabi-v7a` 两个 ABI；生成的 `libzygisk-preback.so` 会自动拷贝到 `module/zygisk/<abi>/`。
3. 将 `module/` 目录打包成 zip，使用 Magisk 导入即可测试。

## 工作原理简述

- 模块在 `postAppSpecialize` 阶段运行，通过反射访问 `ActivityThread.mBoundApplication.appInfo` 与 `LoadedApk.mApplicationInfo`，统一写入 `privateFlagsExt |= 1 << 3`。
- 仅在 `SDK_INT >= 33` 时生效，并默认跳过 `com.android.systemui` 与本项目自带的管理 App 进程。
- 当前 PoC 没有提供黑名单/白名单或远程配置机制，如需扩展可在此基础上继续开发。

> ⚠️ 该 PoC 仅用于验证强制打开预测性返回标志是否可行，尚未针对 OEM 定制或未来 Android 版本做适配，仍需自行评估稳定性。
