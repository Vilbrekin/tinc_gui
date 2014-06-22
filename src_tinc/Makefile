# written by Benjamin Schroedl 2014 - benjamin@dev-tec.de

REQUIREMENTS := autoconf automake bash openssl tar wget

PWD 		:= $(shell pwd)
GIT_ROOT    	:= $(shell git rev-parse --show-toplevel)
SHELL		:= /bin/bash
ARCH 		?= "ARM"
ARCHFILE	:= ".ARCH"
LASTARCH	:=$(shell if [ -e $(ARCHFILE) ] ; then cat $(ARCHFILE); fi;)

# Default to a standard ndk location to error if not present
ANDROID_NDK   ?= /opt/android-ndk
TOOLCHAIN_DIR := $(PWD)/toolchain
TINC_DIR      := $(PWD)/tinc
OPENSSL_DIR   := $(PWD)/openssl
LOG_FILE      := $(PWD)/build.log

CHECK_REQUIREMENT = if [ ! $(shell which $(1)) ]; then echo required program $(1) not found ... please install  ; exit 1 ; fi;

OPENSSL_LIB := $(OPENSSL_DIR)/libcrypto.a

all: prerequisite clean tincd-binary tinc-version
tincd-binary: $(TINC_DIR)/src/tincd

clean:
	@echo "OPENSSL: Cleaning..."
	@$(MAKE) clean -C $(OPENSSL_DIR) > /dev/null
	@echo "TINC: Cleaning..."
	@$(MAKE) clean -C $(TINC_DIR) > /dev/null

#Delete everything including toolchain
mrproper: clean
	@rm -rf $(LOG_FILE) $(TOOLCHAIN_DIR)
	@cd $(GIT_ROOT) && git submodule deinit -f .


# Openssl compiled library (Force -j1 because of openssl limitation)
$(OPENSSL_LIB): $(OPENSSL_DIR)/Makefile $(TOOLCHAIN_DIR) .FORCE
	@echo "OPENSSL: compiling"

	@if [ $(ARCH) == "ARM" ] ; then \
		$(MAKE) -j1 -C $(OPENSSL_DIR) CC=$(TOOLCHAIN_DIR)/bin/arm-linux-androideabi-gcc AR="$(TOOLCHAIN_DIR)/bin/arm-linux-androideabi-ar r" RANLIB=$(TOOLCHAIN_DIR)/bin/arm-linux-androideabi-ranlib  &>> $(LOG_FILE) ; \
	elif [ $(ARCH) == "MIPS" ] ; then \
		$(MAKE) -j1 -C $(OPENSSL_DIR) CC=$(TOOLCHAIN_DIR)/bin/mipsel-linux-android-gcc  AR="$(TOOLCHAIN_DIR)/bin/mipsel-linux-android-ar r"  RANLIB=$(TOOLCHAIN_DIR)/bin/mipsel-linux-android-ranlib   &>> $(LOG_FILE) ; \
	elif [ $(ARCH) == "X86" ] ; then \
		$(MAKE) -j1 -C $(OPENSSL_DIR) CC=$(TOOLCHAIN_DIR)/bin/i686-linux-android-gcc    AR="$(TOOLCHAIN_DIR)/bin/i686-linux-android-ar r"    RANLIB=$(TOOLCHAIN_DIR)/bin/i686-linux-android-ranlib     &>> $(LOG_FILE) ; \
	fi;

# Openssl sources directory
$(OPENSSL_DIR)/Makefile: | git-submodules
	@echo "OPENSSL: Configuring..."
	@cd $(OPENSSL_DIR) && ./Configure dist >> $(LOG_FILE) 

# Install local ARM toolchain
$(TOOLCHAIN_DIR): $(ANDROID_NDK)
	@if [ $(ARCH) == "ARM" ] ; then \
		$(ANDROID_NDK)/build/tools/make-standalone-toolchain.sh --platform=android-5 --arch=arm   --install-dir=$(TOOLCHAIN_DIR) >> $(LOG_FILE) ; \
	elif [ $(ARCH) == "MIPS" ] ; then \
		$(ANDROID_NDK)/build/tools/make-standalone-toolchain.sh --platform=android-9 --arch=mips  --install-dir=$(TOOLCHAIN_DIR) >> $(LOG_FILE) ; \
	elif [ $(ARCH) == "X86" ] ; then \
		$(ANDROID_NDK)/build/tools/make-standalone-toolchain.sh --platform=android-9 --arch=x86   --install-dir=$(TOOLCHAIN_DIR) >> $(LOG_FILE) ; \
	fi;

# tincd binary
$(TINC_DIR)/src/tincd: $(TINC_DIR)/Makefile .FORCE
	@echo "TINC: building tinc ... this may take some time"
	@$(MAKE) -C $(TINC_DIR) >> $(LOG_FILE)
	@echo "TINC: Stripping binary..."

	@if [ $(ARCH) == "ARM" ] ; then \
		$(TOOLCHAIN_DIR)/bin/arm-linux-androideabi-strip $@ ; \
	elif [ $(ARCH) == "MIPS" ] ; then \
		$(TOOLCHAIN_DIR)/bin/mipsel-linux-android-strip $@ ; \
	elif [ $(ARCH) == "X86" ] ; then \
		$(TOOLCHAIN_DIR)/bin/i686-linux-android-strip $@ ; \
	fi;

	@if [ -f $@ ] ; then \
		echo "TINC: tincd can be found in $@"; \
	else \
		echo "TINC: tincd build failed - log file is in ./log/build.log" ;\
	fi;
	

# Download & configure tincd (use order-only dep to avoid taking into account timestamp of the directory)
$(TINC_DIR)/Makefile: $(TOOLCHAIN_DIR) $(OPENSSL_LIB) | git-submodules
	@echo "TINC: Configuring..."
	
	@if [ $(ARCH) == "ARM" ] ; then \
		cd tinc && autoreconf -fsi &>> $(LOG_FILE) && CC=$(TOOLCHAIN_DIR)/bin/arm-linux-androideabi-gcc ./configure --host=arm-linux --disable-lzo  --with-openssl-lib=$(OPENSSL_DIR) --with-openssl-include=$(OPENSSL_DIR)/include/ --disable-hardening &>> $(LOG_FILE) ; \
	elif [ $(ARCH) == "MIPS" ] ; then \
		cd tinc && autoreconf -fsi &>> $(LOG_FILE) && CC=$(TOOLCHAIN_DIR)/bin/mipsel-linux-android-gcc  ./configure --host=mips-linux --disable-lzo --with-openssl-lib=$(OPENSSL_DIR) --with-openssl-include=$(OPENSSL_DIR)/include/ --disable-hardening &>> $(LOG_FILE) ; \
	elif [ $(ARCH) == "X86" ] ; then \
		cd tinc && autoreconf -fsi &>> $(LOG_FILE) && CC=$(TOOLCHAIN_DIR)/bin/i686-linux-android-gcc    ./configure --host=i686-linux --disable-lzo --with-openssl-lib=$(OPENSSL_DIR) --with-openssl-include=$(OPENSSL_DIR)/include/ --disable-hardening &>> $(LOG_FILE) ; \
	fi;

$(ANDROID_NDK):
	@echo "You're ANDROID_NDK variable is not pointing to a valid NDK setup."
	@echo " cd /tmp && wget http://dl.google.com/android/ndk/android-ndk-r9d-linux-x86.tar.bz2 && tar xfj android-ndk-r9d-linux-x86.tar.bz2"
	@echo " ANDROID_NDK=/tmp/android-ndk-r9d make"
	@exit 1
	
git-submodules:
	@echo "Initializing git submodules..."
	@cd $(GIT_ROOT) && git submodule init && git submodule update
    
tinc-version: tincd-binary
	$(eval TINCVER := $(shell grep "^VERSION" $(TINC_DIR)/Makefile ))
	@echo TINC: $(TINCVER)

install: tincd-binary
	mkdir -p $(GIT_ROOT)/res/raw
	@if [ $(ARCH) == "ARM" ] ; then \
		cp $(TINC_DIR)/src/tincd $(GIT_ROOT)/res/raw/tincd.armeabi ; \
	elif [ $(ARCH) == "MIPS" ] ; then \
		cp $(TINC_DIR)/src/tincd $(GIT_ROOT)/res/raw/tincd.mips ; \
	elif [ $(ARCH) == "X86" ] ; then \
		cp $(TINC_DIR)/src/tincd $(GIT_ROOT)/res/raw/tincd.x86 ; \
	fi;

prerequisite:
	@$(foreach req,$(REQUIREMENTS),$(call CHECK_REQUIREMENT,$(req)))
	@touch $(LOG_FILE)
	@touch $(ARCHFILE)

	@if [ $(ARCH) == "ARM" ] || [ $(ARCH) == "MIPS" ] || [ $(ARCH) == "X86" ] ; then \
		echo 'compiling for: $(ARCH)' ; \
		if [ "$(LASTARCH)" != "$(ARCH)" ] ; then \
			rm -r $(TOOLCHAIN_DIR) ; \
		fi; \
		echo "$(ARCH)" > $(ARCHFILE) ; \
	else \
		echo 'unknown arch $(ARCH) - currently supported: ARM / MIPS / X86'; \
		exit 1; \
	fi;
    
.PHONY: all mrproper tincd-binary install prerequisite clean tinc-version git-submodules .FORCE
.FORCE:
