# written by Benjamin Schroedl 2014 - benjamin@dev-tec.de


# Pakages
PKG_ANDROIDNDK_SRC	    = "http://dl.google.com/android/ndk/android-ndk-r9d-linux-x86.tar.bz2"
PKG_ANDROIDNDK_CHECKSUM	= "10feefd8c1ba950a177ce7a165a12d3b89d9822f"
PKG_OPENSSL_SRC		    = "http://www.openssl.org/source/openssl-1.0.1f.tar.gz"
PKG_OPENSSL_CHECKSUM	= "9ef09e97dfc9f14ac2c042f3b7e301098794fc0f"

REQUIREMENTS := autoconf automake bash openssl tar wget

PWD 		:= $(shell pwd)
BUILD_DIR   := $(PWD)/tmp
OPENSSL_FILE 	:= $(BUILD_DIR)/$(shell basename $(PKG_OPENSSL_SRC))
OPENSSL_DIR 	:= $(BUILD_DIR)/$(shell basename $(PKG_OPENSSL_SRC) .tar.gz)
ANDROIDNDK_FILE	:= $(BUILD_DIR)/$(shell basename $(PKG_ANDROIDNDK_SRC))
ANDROIDNDK_DIR	:= $(BUILD_DIR)/$(shell basename $(PKG_ANDROIDNDK_SRC) -linux-x86.tar.bz2)
# Re-use env ANDROID_NDK if it exists already
ANDROID_NDK   ?= $(ANDROIDNDK_DIR)
TOOLCHAIN_DIR := $(PWD)/toolchain
TINC_DIR      := $(PWD)/tinc
LOG_FILE      := $(PWD)/build.log
SHELL		 = bash

PKG_CHECKSUM     = openssl sha1 '$(1)' 2>/dev/null | sed -n 's,^.*\([0-9a-f]\{40\}\)$$,\1,p'
CHECK_REQUIREMENT = if [ ! $(shell which $(1)) ]; then echo required program $(1) not found ... please install  ; exit 1 ; fi;


OPENSSL_LIB := $(OPENSSL_DIR)/libcrypto.a

all: prerequisite tincd-binary tinc-version
tincd-binary: $(TINC_DIR)/src/tincd

clean:
	@echo "OPENSSL: Cleaning..."
	@$(MAKE) clean -C $(OPENSSL_DIR) > /dev/null
	@echo "TINC: Cleaning..."
	@$(MAKE) clean -C $(TINC_DIR) > /dev/null

#Delete everything except tarballs
mrproper: clean
	@rm -rf $(LOG_FILE) $(TOOLCHAIN_DIR) $(OPENSSL_DIR) $(ANDROIDNDK_DIR)


# Openssl compiled library (Force -j1 because of openssl limitation)
$(OPENSSL_LIB): $(OPENSSL_DIR)/Makefile $(TOOLCHAIN_DIR) .FORCE
	@echo "OPENSSL: compiling"
	@$(MAKE) -j1 -C $(OPENSSL_DIR) CC=$(TOOLCHAIN_DIR)/bin/arm-linux-androideabi-gcc AR="$(TOOLCHAIN_DIR)/bin/arm-linux-androideabi-ar r" RANLIB=$(TOOLCHAIN_DIR)/bin/arm-linux-androideabi-ranlib &>> $(LOG_FILE)

# Openssl sources directory
$(OPENSSL_DIR)/Makefile: $(OPENSSL_FILE)
	@echo "OPENSSL: extracting ... this may take some time"
	@tar xfz $(OPENSSL_FILE) -C $(BUILD_DIR)
	@touch $@
	@echo "OPENSSL: Configuring..."
	@cd $(OPENSSL_DIR) && ./Configure dist >> $(LOG_FILE) 

	
# Openssl sources tarball
$(OPENSSL_FILE):
	@echo "OPENSSL: downloading source files"
	wget $(PKG_OPENSSL_SRC) -P $(BUILD_DIR)
	
	@if [ $(PKG_OPENSSL_CHECKSUM) == "`$(call PKG_CHECKSUM,$(OPENSSL_FILE))`" ] ; then \
		echo "OPENSSL: checksum ok" ; \
	else \
		echo "OPENSSL: wrong checksum - aborting ... "; \
		exit 1; \
	fi;

# Install local ARM toolchain
$(TOOLCHAIN_DIR): $(ANDROID_NDK)
	@$(ANDROID_NDK)/build/tools/make-standalone-toolchain.sh --platform=android-5 --install-dir=$(TOOLCHAIN_DIR) >> $(LOG_FILE) 

# Local Android NDK
$(ANDROIDNDK_DIR): $(ANDROIDNDK_FILE)
	@echo "Android NDK: extracting ... this may take some time"
	@tar xfj $(ANDROIDNDK_FILE) -C $(BUILD_DIR)
	@touch $@

# NDK tarball
$(ANDROIDNDK_FILE):
	wget $(PKG_ANDROIDNDK_SRC) -P $(BUILD_DIR)
	if [ $(PKG_ANDROIDNDK_CHECKSUM) == "`$(call PKG_CHECKSUM,$(ANDROIDNDK_FILE))`" ] ; then \
		echo "Android NDK: checksum ok" ; \
	else \
		echo "Android NDK: wrong checksum - aborting ... "; \
		exit 1; \
	fi


# tincd binary
$(TINC_DIR)/src/tincd: $(TINC_DIR)/Makefile .FORCE
	@echo "TINC: building tinc ... this may take some time"
	@$(MAKE) -C $(TINC_DIR) >> $(LOG_FILE)
	@echo "TINC: Stripping binary..."
	@$(TOOLCHAIN_DIR)/bin/arm-linux-androideabi-strip $@

	@if [ -f $@ ] ; then \
		echo "TINC: tincd can be found in $@"; \
	else \
		echo "TINC: tincd build failed - log file is in ./log/build.log" ;\
	fi;
	

# Download & configure tincd (use order-only dep to avoid taking into account timestamp of the directory)
$(TINC_DIR)/Makefile: $(TOOLCHAIN_DIR) $(OPENSSL_LIB) | $(TINC_DIR)
	@echo "TINC: Configuring..."
	@cd tinc && autoreconf -fsi &>> $(LOG_FILE) && \
	CC=$(TOOLCHAIN_DIR)/bin/arm-linux-androideabi-gcc ./configure \
	--host=arm-linux \
	--disable-lzo \
	--with-openssl-lib=$(OPENSSL_DIR) \
	--with-openssl-include=$(OPENSSL_DIR)/include/ --disable-hardening &>> $(LOG_FILE) 

tinc-version: tincd-binary
	$(eval TINCVER := $(shell grep "^VERSION" $(TINC_DIR)/Makefile ))
	@echo TINC: $(TINCVER)

install: tincd-binary
	cp $(TINC_DIR)/src/tincd $(PWD)/../res/raw/tincd

prerequisite:
	@$(foreach req,$(REQUIREMENTS),$(call CHECK_REQUIREMENT,$(req)))

	@mkdir -p $(BUILD_DIR)
	@touch $(LOG_FILE)

    
.PHONY: all mrproper tincd-binary install prerequisite clean tinc-version .FORCE
.FORCE:
