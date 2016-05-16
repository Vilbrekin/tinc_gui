Tinc GUI for android.
http://tinc_gui.poirsouille.org/.

Tinc is a peer to peer VPN daemon.

Using tinc daemon, cross-compiled for android.
See https://github.com/Vilbrekin/tinc and original web site http://tinc-vpn.org/.

Copyright (C) 2016 Vilbrekin <vilbrekin@gmail.com>
Distributed under GPLv3.

[![Build Status](https://travis-ci.org/Vilbrekin/tinc_gui.svg?branch=master)](https://travis-ci.org/Vilbrekin/tinc_gui)

tincd binary can be cross-compiled ing the NDK.
Git sub-modules are used to track the correct versions of OpenSSL and tinc.
The easiest way to build tincd should be similar to:

```
git submodule update --init
./gradlew build
```

