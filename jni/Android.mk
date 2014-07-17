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
	ed25519/ed25519.h \
	ed25519/fe.c ed25519/fe.h \
	ed25519/fixedint.h \
	ed25519/ge.c ed25519/ge.h \
	ed25519/key_exchange.c \
	ed25519/keypair.c \
	ed25519/precomp_data.h \
	ed25519/sc.c ed25519/sc.h \
	ed25519/seed.c \
	ed25519/sha512.c ed25519/sha512.h \
	ed25519/sign.c \
	ed25519/verify.c

chacha_poly1305_SOURCES = \
	chacha-poly1305/chacha.c chacha-poly1305/chacha.h \
	chacha-poly1305/chacha-poly1305.c chacha-poly1305/chacha-poly1305.h \
	chacha-poly1305/poly1305.c chacha-poly1305/poly1305.h

tincd_SOURCES = \
	buffer.c buffer.h \
	cipher.h \
	conf.c conf.h \
	connection.c connection.h \
	control.c control.h \
	control_common.h \
	crypto.h \
	device.h \
	digest.h \
	dropin.c dropin.h \
	dummy_device.c \
	ecdh.h \
	ecdsa.h \
	ecdsagen.h \
	edge.c edge.h \
	ethernet.h \
	event.c event.h \
	fake-gai-errnos.h \
	fake-getaddrinfo.c fake-getaddrinfo.h \
	fake-getnameinfo.c fake-getnameinfo.h \
	getopt.c getopt.h \
	getopt1.c \
	graph.c graph.h \
	hash.c hash.h \
	have.h \
	ipv4.h \
	ipv6.h \
	list.c list.h \
	logger.c logger.h \
	meta.c meta.h \
	multicast_device.c \
	names.c names.h \
	net.c net.h \
	net_packet.c \
	net_setup.c \
	net_socket.c \
	netutl.c netutl.h \
	node.c node.h \
	prf.h \
	process.c process.h \
	protocol.c protocol.h \
	protocol_auth.c \
	protocol_edge.c \
	protocol_key.c \
	protocol_misc.c \
	protocol_subnet.c \
	raw_socket_device.c \
	route.c route.h \
	rsa.h \
	rsagen.h \
	script.c script.h \
	splay_tree.c splay_tree.h \
	sptps.c sptps.h \
	subnet.c subnet.h \
	subnet_parse.c \
	system.h \
	tincd.c \
	utils.c utils.h \
	xalloc.h \
	version.c version.h \
	$(ed25519_SOURCES) \
	$(chacha_poly1305_SOURCES)

tincd_SOURCES += linux/device.c

tincd_SOURCES += \
	openssl/cipher.c \
	openssl/crypto.c \
	openssl/digest.c openssl/digest.h \
	ed25519/ecdh.c \
	ed25519/ecdsa.c \
	openssl/prf.c \
	openssl/rsa.c

LOCAL_SRC_FILES += $(addprefix tinc/src/,$(tincd_SOURCES))
#LOCAL_SRC_FILES += tinc/src/hash.c tinc/src/splay_tree.c tinc/src/ed25519/ecdsa.c tinc/src/sptps.c tinc/src/subnet_parse.c tinc/src/script.c tinc/src/openssl/cipher.c tinc/src/openssl/digest.c tinc/src/buffer.c tinc/src/control.c tinc/src/openssl/rsa.c tinc/src/openssl/crypto.c tinc/src/names.c tinc/src/protocol_edge.c tinc/src/dummy_device.c tinc/src/protocol_subnet.c tinc/src/edge.c tinc/src/conf.c tinc/src/protocol_misc.c tinc/src/utils.c tinc/src/event.c tinc/src/protocol_key.c tinc/src/raw_socket_device.c tinc/src/protocol.c tinc/src/net_socket.c tinc/src/logger.c tinc/src/list.c tinc/src/fake-getaddrinfo.c tinc/src/net.c tinc/src/tincd.c tinc/src/graph.c tinc/src/process.c tinc/src/net_packet.c tinc/src/netutl.c tinc/src/linux/device.c tinc/src/route.c tinc/src/meta.c tinc/src/protocol_auth.c tinc/src/net_setup.c tinc/src/getopt.c tinc/src/multicast_device.c tinc/src/connection.c tinc/src/subnet.c tinc/src/node.c tinc/src/fake-getnameinfo.c tinc/src/getopt1.c tinc/src/dropin.c
OPENSSL_DIR := $(NDK_PROJECT_PATH)/platform_external_openssl
LOCAL_C_INCLUDES := $(OPENSSL_DIR)/include

# Hack around missing #ifdef HAVE_CONFIG_H in system.h and relative paths includes (so that it finds ../config.h file)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/tinc

include $(BUILD_EXECUTABLE)

# Dependency on OpenSSL
include platform_external_openssl/Android.mk
