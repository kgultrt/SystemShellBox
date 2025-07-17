LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := ssb_daemon
LOCAL_SRC_FILES := main.c
LOCAL_CFLAGS    := -Wall -Wextra -O2 -D_FILE_OFFSET_BITS=64 -D_BSD_SOURCE
LOCAL_LDLIBS    := -llog -landroid

include $(BUILD_SHARED_LIBRARY)