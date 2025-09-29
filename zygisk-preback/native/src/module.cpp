#include <android/log.h>
#include <cstring>
#include "zygisk.hpp"

namespace {
constexpr const char *kTag = "PreBackZygisk";
constexpr jint kEnableOnBackAppFlag = 1 << 3; // ApplicationInfo.PRIVATE_FLAG_EXT_ENABLE_ON_BACK_INVOKED_CALLBACK
constexpr jint kEnableOnBackActivityFlag = 1 << 2; // ActivityInfo.PRIVATE_FLAG_ENABLE_ON_BACK_INVOKED_CALLBACK
constexpr jint kDisableOnBackActivityFlag = 1 << 3; // ActivityInfo.PRIVATE_FLAG_DISABLE_ON_BACK_INVOKED_CALLBACK
constexpr jint kAndroid13 = 33;

jint getSdkInt(JNIEnv *env) {
    jclass versionClass = env->FindClass("android/os/Build$VERSION");
    if (!versionClass) {
        env->ExceptionClear();
        return 0;
    }
    jfieldID sdkField = env->GetStaticFieldID(versionClass, "SDK_INT", "I");
    if (!sdkField) {
        env->ExceptionClear();
        env->DeleteLocalRef(versionClass);
        return 0;
    }
    jint sdk = env->GetStaticIntField(versionClass, sdkField);
    env->DeleteLocalRef(versionClass);
    return sdk;
}

void setApplicationFlag(JNIEnv *env, jobject appInfo) {
    if (!appInfo) {
        return;
    }
    jclass appInfoClass = env->GetObjectClass(appInfo);
    if (!appInfoClass) {
        env->ExceptionClear();
        return;
    }
    jfieldID privateFlagsExtField = env->GetFieldID(appInfoClass, "privateFlagsExt", "I");
    if (!privateFlagsExtField) {
        env->ExceptionClear();
        env->DeleteLocalRef(appInfoClass);
        return;
    }

    jint flags = env->GetIntField(appInfo, privateFlagsExtField);
    if ((flags & kEnableOnBackAppFlag) == 0) {
        flags |= kEnableOnBackAppFlag;
        env->SetIntField(appInfo, privateFlagsExtField, flags);
        __android_log_print(ANDROID_LOG_DEBUG, kTag, "privateFlagsExt updated -> %d", flags);
    }
    env->DeleteLocalRef(appInfoClass);
}

void setActivityFlag(JNIEnv *env, jobject activityInfo) {
    if (!activityInfo) {
        return;
    }
    jclass activityInfoClass = env->GetObjectClass(activityInfo);
    if (!activityInfoClass) {
        env->ExceptionClear();
        return;
    }
    jfieldID privateFlagsField = env->GetFieldID(activityInfoClass, "privateFlags", "I");
    if (!privateFlagsField) {
        env->ExceptionClear();
        env->DeleteLocalRef(activityInfoClass);
        return;
    }
    jint flags = env->GetIntField(activityInfo, privateFlagsField);
    bool updated = false;
    if ((flags & kEnableOnBackActivityFlag) == 0) {
        flags |= kEnableOnBackActivityFlag;
        updated = true;
    }
    if ((flags & kDisableOnBackActivityFlag) != 0) {
        flags &= ~kDisableOnBackActivityFlag;
        updated = true;
    }
    if (updated) {
        env->SetIntField(activityInfo, privateFlagsField, flags);
        __android_log_print(ANDROID_LOG_DEBUG, kTag, "activity privateFlags updated -> %d", flags);
    }
    env->DeleteLocalRef(activityInfoClass);
}

void patchLoadedApk(JNIEnv *env, jobject bindData) {
    if (!bindData) {
        return;
    }
    jclass bindDataClass = env->GetObjectClass(bindData);
    if (!bindDataClass) {
        env->ExceptionClear();
        return;
    }

    jfieldID appInfoField = env->GetFieldID(bindDataClass, "appInfo", "Landroid/content/pm/ApplicationInfo;");
    if (appInfoField) {
        jobject appInfo = env->GetObjectField(bindData, appInfoField);
        if (appInfo) {
            setApplicationFlag(env, appInfo);
            env->DeleteLocalRef(appInfo);
        }
    } else {
        env->ExceptionClear();
    }

    jfieldID loadedApkField = env->GetFieldID(bindDataClass, "info", "Landroid/app/LoadedApk;");
    if (loadedApkField) {
        jobject loadedApk = env->GetObjectField(bindData, loadedApkField);
        if (loadedApk) {
            jclass loadedApkClass = env->GetObjectClass(loadedApk);
            if (loadedApkClass) {
                jfieldID laAppInfoField = env->GetFieldID(loadedApkClass, "mApplicationInfo", "Landroid/content/pm/ApplicationInfo;");
                if (laAppInfoField) {
                    jobject laAppInfo = env->GetObjectField(loadedApk, laAppInfoField);
                    if (laAppInfo) {
                        setApplicationFlag(env, laAppInfo);
                        env->DeleteLocalRef(laAppInfo);
                    }
                } else {
                    env->ExceptionClear();
                }

                // Update cached ActivityInfo instances if present
                jfieldID activityInfoMapField = env->GetFieldID(loadedApkClass, "mActivityInfo", "Landroid/util/ArrayMap;");
                if (activityInfoMapField) {
                    jobject activityInfoMap = env->GetObjectField(loadedApk, activityInfoMapField);
                    if (activityInfoMap) {
                        jclass arrayMapClass = env->FindClass("android/util/ArrayMap");
                        if (arrayMapClass) {
                            jmethodID sizeMethod = env->GetMethodID(arrayMapClass, "size", "()I");
                            jmethodID valueAtMethod = env->GetMethodID(arrayMapClass, "valueAt", "(I)Ljava/lang/Object;");
                            if (sizeMethod && valueAtMethod) {
                                jint size = env->CallIntMethod(activityInfoMap, sizeMethod);
                                if (!env->ExceptionCheck()) {
                                    for (jint i = 0; i < size; ++i) {
                                        jobject info = env->CallObjectMethod(activityInfoMap, valueAtMethod, i);
                                        if (env->ExceptionCheck()) {
                                            env->ExceptionClear();
                                            break;
                                        }
                                        if (info) {
                                            setActivityFlag(env, info);
                                            env->DeleteLocalRef(info);
                                        }
                                    }
                                } else {
                                    env->ExceptionClear();
                                }
                            } else {
                                env->ExceptionClear();
                            }
                            env->DeleteLocalRef(arrayMapClass);
                        } else {
                            env->ExceptionClear();
                        }
                        env->DeleteLocalRef(activityInfoMap);
                    }
                } else {
                    env->ExceptionClear();
                }
                env->DeleteLocalRef(loadedApkClass);
            } else {
                env->ExceptionClear();
            }
            env->DeleteLocalRef(loadedApk);
        }
    } else {
        env->ExceptionClear();
    }
    env->DeleteLocalRef(bindDataClass);
}

bool shouldSkipProcess(JNIEnv *env, const zygisk::AppSpecializeArgs *args) {
    if (!args || !args->nice_name) {
        return false;
    }
    const char *name = env->GetStringUTFChars(args->nice_name, nullptr);
    if (!name) {
        env->ExceptionClear();
        return false;
    }
    bool skip = false;
    if (name[0] == '\0') {
        skip = true;
    } else if (strncmp(name, "com.android.systemui", 21) == 0) {
        skip = true;
    } else if (strncmp(name, "com.xeasy.prebackanim", 22) == 0) {
        skip = true;
    }
    env->ReleaseStringUTFChars(args->nice_name, name);
    return skip;
}

void applyFlagToCurrentProcess(JNIEnv *env) {
    jclass activityThreadClass = env->FindClass("android/app/ActivityThread");
    if (!activityThreadClass) {
        env->ExceptionClear();
        return;
    }
    jmethodID currentThreadMethod = env->GetStaticMethodID(activityThreadClass, "currentActivityThread", "()Landroid/app/ActivityThread;");
    if (!currentThreadMethod) {
        env->ExceptionClear();
        env->DeleteLocalRef(activityThreadClass);
        return;
    }
    jobject activityThread = env->CallStaticObjectMethod(activityThreadClass, currentThreadMethod);
    if (!activityThread) {
        env->DeleteLocalRef(activityThreadClass);
        return;
    }
    jfieldID boundAppField = env->GetFieldID(activityThreadClass, "mBoundApplication", "Landroid/app/ActivityThread$AppBindData;");
    if (!boundAppField) {
        env->ExceptionClear();
        env->DeleteLocalRef(activityThread);
        env->DeleteLocalRef(activityThreadClass);
        return;
    }
    jobject bindData = env->GetObjectField(activityThread, boundAppField);
    if (bindData) {
        patchLoadedApk(env, bindData);
        env->DeleteLocalRef(bindData);
    }
    env->DeleteLocalRef(activityThread);
    env->DeleteLocalRef(activityThreadClass);
}

class PreBackModule : public zygisk::ModuleBase {
public:
    void onLoad(zygisk::Api *api, JNIEnv *env) override {
        api_ = api;
        env->GetJavaVM(&vm_);
        __android_log_print(ANDROID_LOG_INFO, kTag, "module loaded");
    }

    void postAppSpecialize(const zygisk::AppSpecializeArgs *args) override {
        if (!vm_) {
            return;
        }
        JNIEnv *env = nullptr;
        bool needDetach = false;
        jint getEnvResult = vm_->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
        if (getEnvResult == JNI_EDETACHED) {
            if (vm_->AttachCurrentThread(&env, nullptr) != JNI_OK) {
                __android_log_print(ANDROID_LOG_ERROR, kTag, "AttachCurrentThread failed");
                return;
            }
            needDetach = true;
        } else if (getEnvResult != JNI_OK) {
            __android_log_print(ANDROID_LOG_ERROR, kTag, "GetEnv failed: %d", getEnvResult);
            return;
        }

        if (shouldSkipProcess(env, args)) {
            if (needDetach) vm_->DetachCurrentThread();
            return;
        }

        if (getSdkInt(env) >= kAndroid13) {
            applyFlagToCurrentProcess(env);
        }

        if (needDetach) {
            vm_->DetachCurrentThread();
        }
    }

private:
    zygisk::Api *api_{};
    JavaVM *vm_{};
};

} // namespace

REGISTER_ZYGISK_MODULE(PreBackModule)
