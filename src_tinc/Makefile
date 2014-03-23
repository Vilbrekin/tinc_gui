# written by Benjamin Schroedl 2014 - benjamin@dev-tec.de

REQUIREMENTS := autoconf automake bash openssl tar wget

PWD 		:= $(shell pwd)
# Default to a standard ndk location to error if not present
ANDROID_NDK   ?= /opt/android-ndk
TOOLCHAIN_DIR := $(PWD)/toolchain
TINC_DIR      := $(PWD)/tinc
OPENSSL_DIR   := $(PWD)/openssl
LOG_FILE      := $(PWD)/build.log

CHECK_REQUIREMENT = if [ ! $(shell which $(1)) ]; then echo required program $(1) not found ... please install  ; exit 1 ; fi;

OPENSSL_LIB := $(OPENSSL_DIR)/libcrypto.a

all: prerequisite tincd-binary tinc-version
tincd-binary: $(TINC_DIR)/src/tincd

clean:
	@echo "OPENSSL: Cleaning..."
	@$(MAKE) clean -C $(OPENSSL_DIR) > /dev/null
	@echo "TINC: Cleaning..."
	@$(MAKE) clean -C $(TINC_DIR) > /dev/null

#Delete everything including toolchain
mrproper: clean
	@rm -rf $(LOG_FILE) $(TOOLCHAIN_DIR)


# Openssl compiled library (Force -j1 because of openssl limitation)
$(OPENSSL_LIB): $(OPENSSL_DIR)/Makefile $(TOOLCHAIN_DIR) .FORCE
	@echo "OPENSSL: compiling"
	@$(MAKE) -j1 -C $(OPENSSL_DIR) CC=$(TOOLCHAIN_DIR)/bin/arm-linux-androideabi-gcc AR="$(TOOLCHAIN_DIR)/bin/arm-linux-androideabi-ar r" RANLIB=$(TOOLCHAIN_DIR)/bin/arm-linux-androideabi-ranlib &>> $(LOG_FILE)

# Openssl sources directory
$(OPENSSL_DIR)/Makefile:
	@echo "OPENSSL: Configuring..."
	@cd $(OPENSSL_DIR) && ./Configure dist >> $(LOG_FILE) 

# Install local ARM toolchain
$(TOOLCHAIN_DIR): $(ANDROID_NDK)
	@$(ANDROID_NDK)/build/tools/make-standalone-toolchain.sh --platform=android-5 --install-dir=$(TOOLCHAIN_DIR) >> $(LOG_FILE) 

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
	mkdir -p $(PWD)/../res/raw
	cp $(TINC_DIR)/src/tincd $(PWD)/../res/raw/tincd

prerequisite:
	@$(foreach req,$(REQUIREMENTS),$(call CHECK_REQUIREMENT,$(req)))
	@touch $(LOG_FILE)
    
.PHONY: all mrproper tincd-binary install prerequisite clean tinc-version .FORCE
.FORCE:
