LOCAL_PATH := $(call my-dir)


include $(CLEAR_VARS)

# Hack around usage of multilib LOCAL_*_arch by android openssl upstream (not sure if they use a customized NDK...). This ensures those variables are correctly cleared by CLEAN_VARS.
modules-LOCALS += SRC_FILES_arm CFLAGS_arm SRC_FILES_arm64 CFLAGS_arm64 SRC_FILES_x86 CFLAGS_x86 SRC_FILES_x86_64 CFLAGS_x86_64 SRC_FILES_mips CFLAGS_mips 

LOCAL_MODULE := tincd
LOCAL_LDFLAGS := 
LOCAL_LDLIBS := -lz
LOCAL_CFLAGS := -std=gnu99 -DHAVE_CONFIG_H -DCONFDIR=\"/etc\" -DLOCALSTATEDIR=\"/var\"
LOCAL_STATIC_LIBRARIES := libcrypto_static libssl_static
LOCAL_FORCE_STATIC_EXECUTABLE := true
LOCAL_SRC_FILES += tinc/src/protocol_edge.c tinc/src/xmalloc.c tinc/src/dummy_device.c tinc/src/protocol_subnet.c tinc/src/edge.c tinc/src/pidfile.c tinc/src/conf.c tinc/src/protocol_misc.c tinc/src/utils.c tinc/src/event.c tinc/src/protocol_key.c tinc/src/raw_socket_device.c tinc/src/protocol.c tinc/src/net_socket.c tinc/src/logger.c tinc/src/list.c tinc/src/fake-getaddrinfo.c tinc/src/net.c tinc/src/tincd.c tinc/src/graph.c tinc/src/process.c tinc/src/net_packet.c tinc/src/netutl.c tinc/src/linux/device.c tinc/src/avl_tree.c tinc/src/route.c tinc/src/meta.c tinc/src/protocol_auth.c tinc/src/net_setup.c tinc/src/getopt.c tinc/src/multicast_device.c tinc/src/connection.c tinc/src/subnet.c tinc/src/node.c tinc/src/fake-getnameinfo.c tinc/src/getopt1.c tinc/src/dropin.c
OPENSSL_DIR := $(NDK_PROJECT_PATH)/jni/platform_external_openssl
LOCAL_C_INCLUDES := $(OPENSSL_DIR)/include

# Hack around missing #ifdef HAVE_CONFIG_H in system.h and relative paths includes (so that it finds ../config.h file)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/tinc

include $(BUILD_EXECUTABLE)

# Dependency on OpenSSL
include platform_external_openssl/Android.mk
