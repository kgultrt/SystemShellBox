LOCAL_PATH := $(call my-dir)

# ELF Loader 模块
include $(CLEAR_VARS)

LOCAL_MODULE := elf_loader

# 获取所有 C 文件
ELF_LOADER_SRC_DIR := $(LOCAL_PATH)/elf_loader
ELF_LOADER_C_FILES := $(wildcard $(ELF_LOADER_SRC_DIR)/*.c)

# 根据目标架构选择对应的汇编文件
TARGET_ARCH_NAME :=

# 映射 Android ABI 到你的目录结构
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
    TARGET_ARCH_NAME := aarch64
    LOCAL_CFLAGS += -DELFCLASS=ELFCLASS64
else ifeq ($(TARGET_ARCH_ABI),x86_64)
    TARGET_ARCH_NAME := amd64
    LOCAL_CFLAGS += -DELFCLASS=ELFCLASS64
else ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    TARGET_ARCH_NAME := arm
    LOCAL_CFLAGS += -DELFCLASS=ELFCLASS32 -DARCH_ARM
    LOCAL_CFLAGS += -march=armv7-a -mfloat-abi=softfp -mfpu=vfpv3-d16
else ifeq ($(TARGET_ARCH_ABI),x86)
    TARGET_ARCH_NAME := i386
    LOCAL_CFLAGS += -m32 -DELFCLASS=ELFCLASS32
endif

# 添加汇编文件
ifneq ($(TARGET_ARCH_NAME),)
    ASM_FILES := $(wildcard $(ELF_LOADER_SRC_DIR)/$(TARGET_ARCH_NAME)/*.S)
    LOCAL_SRC_FILES := $(ELF_LOADER_C_FILES:$(LOCAL_PATH)/%=%) \
                       $(ASM_FILES:$(LOCAL_PATH)/%=%)
    
    # 定义编译宏
    LOCAL_CFLAGS += -DZ_ARCH_$(TARGET_ARCH_NAME)
else
    $(error Unsupported ABI: $(TARGET_ARCH_ABI))
endif

# 添加必要的编译标志（来自原 Makefile）
LOCAL_CFLAGS += -pipe -Wall -Wextra -fPIC -fno-ident
LOCAL_CFLAGS += -fno-stack-protector -U _FORTIFY_SOURCE
LOCAL_CFLAGS += -fvisibility=hidden
LOCAL_CFLAGS += -fno-asynchronous-unwind-tables -fno-unwind-tables
LOCAL_CFLAGS += -ffunction-sections -fdata-sections
LOCAL_CFLAGS += -Wl,--gc-sections

# 链接标志（来自原 Makefile）
LOCAL_LDFLAGS += -nostartfiles -nodefaultlibs -nostdlib
LOCAL_LDFLAGS += -pie -Wl,-Bsymbolic,--no-undefined,--build-id=none
LOCAL_LDFLAGS += -Wl,--gc-sections

# 如果 loader 需要入口点设置
LOCAL_LDFLAGS += -Wl,-e,z_start

# Android 特定的链接库（只有 log，因为不能用标准库）
LOCAL_LDLIBS := -llog

# 禁用某些警告
LOCAL_CFLAGS += -Wno-unused-parameter -Wno-unused-variable -Wno-format
LOCAL_CFLAGS += -Wno-pointer-sign

include $(BUILD_SHARED_LIBRARY)

# File Utils 模块（保持不变）
include $(CLEAR_VARS)
LOCAL_MODULE := file_utils
LOCAL_SRC_FILES := file_action/main.c
LOCAL_CFLAGS := -Wall -Wextra -O2 -D_FILE_OFFSET_BITS=64 -D_BSD_SOURCE
LOCAL_LDLIBS := -llog -landroid
include $(BUILD_SHARED_LIBRARY)