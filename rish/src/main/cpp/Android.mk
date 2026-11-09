LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE           := rish
LOCAL_SRC_FILES        := main.cpp pts.cpp rikka_rish_RishTerminal.cpp rikka_rish_RishHost.cpp
LOCAL_LDLIBS           := -llog
LOCAL_STATIC_LIBRARIES := cxx
include $(BUILD_SHARED_LIBRARY)

$(call import-module,prefab/cxx)
