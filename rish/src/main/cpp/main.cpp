#include <jni.h>
#include "rikka_rish_RishTerminal.h"
#include "rikka_rish_RishHost.h"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK)
        return -1;

    if (rikka_rish_RishHost_registerNatives(env) != JNI_OK
        || rikka_rish_RishTerminal_registerNatives(env) != JNI_OK) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
