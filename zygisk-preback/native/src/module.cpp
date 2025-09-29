#include <jni.h>
#include <android/log.h>
#include <cstring>
#include "zygisk.hpp"

namespace {
constexpr const char *kTag = "PreBackZygisk";
constexpr jint kEnableOnBackFlag = 1 << 3;
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

void setFlag(JNIEnv *env, jobject appInfo) {
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
    if ((flags & kEnableOnBackFlag) == 0) {
        flags |= kEnableOnBackFlag;
        env->SetIntField(appInfo, privateFlagsExtField, flags);
        __android_log_print(ANDROID_LOG_DEBUG, kTag, "privateFlagsExt updated -> %d", flags);
    }
    env->DeleteLocalRef(appInfoClass);
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
            setFlag(env, appInfo);
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
                        setFlag(env, laAppInfo);
                        env->DeleteLocalRef(laAppInfo);
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
    if (name[0] == 0) {
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
