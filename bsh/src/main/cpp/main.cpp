#include <jni.h>
#include "rikka_bsh_BSHTerminal.h"
#include "rikka_bsh_BSHHost.h"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK)
        return -1;

    if (rikka_bsh_BSHTerminal_registerNatives(env) != JNI_OK
        || rikka_bsh_BSHHost_registerNatives(env) != JNI_OK) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
