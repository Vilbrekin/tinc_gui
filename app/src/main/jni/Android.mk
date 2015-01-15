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

ed25519_SOURCES = \
	ed25519/add_scalar.c \
	ed25519/fe.c \
	ed25519/ge.c \
	ed25519/key_exchange.c \
	ed25519/keypair.c \
	ed25519/sc.c \
	ed25519/sha512.c \
	ed25519/sign.c \
	ed25519/verify.c

chacha_poly1305_SOURCES = \
	chacha-poly1305/chacha.c \
	chacha-poly1305/chacha-poly1305.c \
	chacha-poly1305/poly1305.c 

tincd_SOURCES = \
	buffer.c \
	conf.c \
	connection.c \
	control.c  \
	dropin.c  \
	dummy_device.c \
	edge.c  \
	event.c  \
	fake-getaddrinfo.c  \
	fake-getnameinfo.c  \
	getopt.c  \
	getopt1.c \
	graph.c  \
	hash.c  \
	list.c  \
	logger.c  \
	meta.c  \
	multicast_device.c \
	names.c  \
	net.c  \
	net_packet.c \
	net_setup.c \
	net_socket.c \
	netutl.c  \
	node.c  \
	process.c  \
	protocol.c  \
	protocol_auth.c \
	protocol_edge.c \
	protocol_key.c \
	protocol_misc.c \
	protocol_subnet.c \
	raw_socket_device.c \
	route.c  \
	script.c  \
	splay_tree.c \
	sptps.c  \
	subnet.c  \
	subnet_parse.c \
	tincd.c \
	utils.c  \
	version.c  \
	$(ed25519_SOURCES) \
	$(chacha_poly1305_SOURCES)

tincd_SOURCES += linux/device.c

tincd_SOURCES += \
	openssl/cipher.c \
	openssl/crypto.c \
	openssl/digest.c \
	ed25519/ecdh.c \
	ed25519/ecdsa.c \
	openssl/prf.c \
	openssl/rsa.c

LOCAL_SRC_FILES += $(addprefix tinc/src/,$(tincd_SOURCES))
OPENSSL_DIR := $(NDK_PROJECT_PATH)/jni/platform_external_openssl
LOCAL_C_INCLUDES := $(OPENSSL_DIR)/include

# Hack around missing #ifdef HAVE_CONFIG_H in system.h and relative paths includes (so that it finds ../config.h file)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/tinc

include $(BUILD_EXECUTABLE)

# Dependency on OpenSSL
include platform_external_openssl/Android.mk
