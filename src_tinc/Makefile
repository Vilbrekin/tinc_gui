# written by Benjamin Schroedl 2014 - benjamin@dev-tec.de


# Pakages
PKG_ANDROIDNDK_SRC	= "http://dl.google.com/android/ndk/android-ndk-r9d-linux-x86.tar.bz2"
PKG_ANDROIDNDK_CHECKSUM	= "10feefd8c1ba950a177ce7a165a12d3b89d9822f"
PKG_OPENSSL_SRC		= "http://www.openssl.org/source/openssl-1.0.0l.tar.gz"
PKG_OPENSSL_CHECKSUM	= "f7aeaa76a043ab9c1cd5899d09c696d98278e2d7"
PKG_TINC_SRC		= "http://tinc-vpn.org/git/tinc"

PWD 		:= $(shell pwd)
OPENSSL_FILE 	:= $(shell basename $(PKG_OPENSSL_SRC))
OPENSSL_DIR 	:= $(shell basename $(PKG_OPENSSL_SRC) .tar.gz)
ANDROIDNDK_FILE	:= $(shell basename $(PKG_ANDROIDNDK_SRC))
ANDROIDNDK_DIR	:= $(shell basename $(PKG_ANDROIDNDK_SRC) -linux-x86.tar.bz2)
SHELL		 = bash


all: prerequisite sdk openssl tinc

clean:
	@if [ -d $(PWD)/toolchain ] ; then rm -r $(PWD)/toolchain ; fi;
	@if [ -d $(PWD)/tinc ]      ; then rm -r $(PWD)/tinc      ; fi;
	@if [ -d $(PWD)/tmp ]       ; then rm -r $(PWD)/tmp       ; fi;

download: openssl-download sdk-download tinc-download


openssl: openssl-download openssl-build

openssl-build:
	@echo "OPENSSL: extracting ... this may take some time"
	@tar xfz $(PWD)/tmp/$(OPENSSL_FILE) -C $(PWD)/tmp/
	
	@echo "OPENSSL: compiling"
	@cd tmp/$(OPENSSL_DIR) && \
	./Configure dist > /dev/null && \
	make CC=$(PWD)/toolchain/bin/arm-linux-androideabi-gcc AR="$(PWD)/toolchain/bin/arm-linux-androideabi-ar r" RANLIB=$(PWD)/toolchain/bin/arm-linux-androideabi-ranlib > /dev/null

openssl-download:
	@if [ -e "$(PWD)/tmp/$(OPENSSL_FILE)" ] ; then \
		echo "OPENSSL: downloaded files seem to be ok" ; \
	else \
		wget $(PKG_OPENSSL_SRC) -P $(PWD)/tmp/ ; \
	fi;
	
	@if [ $(PKG_OPENSSL_CHECKSUM) == "$(shell openssl sha1 '$(PWD)/tmp/$(OPENSSL_FILE)' 2>/dev/null | sed -n 's,^.*\([0-9a-f]\{40\}\)$$,\1,p')" ] ; then \
		echo "OPENSSL: checksum ok" ; \
	else \
		echo "OPENSSL: wrong checksum - aborting ... "; \
		exit 1; \
	fi;


sdk: sdk-download sdk-build

sdk-build:
	@if [ -d $(PWD)/toolchain ] ; then \
		rm -r $(PWD)/toolchain ; \
	fi;
	
	@echo "Android NDK: extracting ... this may take some time"
	@tar xfj $(PWD)/tmp/$(ANDROIDNDK_FILE) -C $(PWD)/tmp/
	
	@echo "Android NDK: installing toolchain to $(PWD)/toolchain"
	@$(PWD)/tmp/$(ANDROIDNDK_DIR)/build/tools/make-standalone-toolchain.sh --platform=android-5 --install-dir=$(PWD)/toolchain  > /dev/null

sdk-download:
	@if [ -e "$(PWD)/tmp/$(ANDROIDNDK_FILE)" ] ; then \
		echo "Android NDK: downloaded files seem to be ok" ; \
	else \
		wget $(PKG_ANDROIDNDK_SRC) -P $(PWD)/tmp/ ; \
	fi;
	
	@if [ $(PKG_ANDROIDNDK_CHECKSUM) == $(shell openssl sha1 '$(PWD)/tmp/$(ANDROIDNDK_FILE)' 2>/dev/null | sed -n 's,^.*\([0-9a-f]\{40\}\)$$,\1,p') ] ; then \
		echo "Android NDK: checksum ok" ; \
	else \
		echo "Android NDK: wrong checksum - aborting ... "; \
		exit 1; \
	fi;

tinc: tinc-download tinc-build

tinc-build:
	cd tinc && autoreconf -fsi

tinc-download:
	@echo "TINC: downloading tinc from git repo"
	@if [ -d $(PWD)/tinc ] ; then \
		cd tinc && git pull > /dev/null ; \
	else \
		git clone $(PKG_TINC_SRC) > /dev/null ; \
	fi;
	
	
	@echo "TINC: building tinc ... this may take some time"
	@cd tinc && \
	CC=$(PWD)/toolchain/bin/arm-linux-androideabi-gcc ./configure \
	--host=arm-linux \
	--disable-lzo \
	--with-openssl-lib=$(PWD)/tmp/$(OPENSSL_DIR) \
	--with-openssl-include=$(PWD)/tmp/$(OPENSSL_DIR)/include/ > /dev/null && \
	make > /dev/null

	@if [ -f $(PWD)/tinc/src/tincd ] ; then \
		echo "TINC: tincd can be found in tinc/src/tincd"; \
	else \
		echo "TINC: tincd build failed" ;\
	fi;
	

install:
	@if [ -f $(PWD)/tinc/src/tincd ] ; then \
		cp $(PWD)/tinc/src/tincd $(PWD)/../raw/tincd ; \
	else \
		echo "tincd binary not found - please run 'make' before 'make install'" ;\
	fi;

prerequisite:
	# check for wget tar
	# basic vars


	@if [ ! -d "$(PWD)/tmp/" ] ; then \
		mkdir -p $(PWD)/tmp/ ; \
	fi
