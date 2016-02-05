# Node Wiring [![Build Status](https://travis-ci.org/INAETICS/node-wiring-c.svg?branch=master)](https://travis-ci.org/INAETICS/node-wiring-c)

This branch contains a Celix-based implementation for the INAETICS wiring capabilities.

## Building and execution instructions

0.   Be sure to have installed cmake,apr,apr-util,zlib,curl and jansson libraries
1.   Download, compile and install Celix (sources can be checked out from  https://svn.apache.org/repos/asf/celix/trunk/. Building and configuring instructions are included.)
2.   Checkout the node-wiring-c source code: git clone https://github.com/INAETICS/node-wiring-c
3.   Create a build folder mkdir node-wiring-build && cd node-wiring-build 
4.   Start cmake with either: cmake -DCELIX_DIR=<celix installation folder>  ..  or: ccmake ..  -DCELIX_DIR=<celix installatin folder> to configure the project via the interactive menu
5.   make all
6.   make deploy
7.   Terminal 1: cd deploy/wiring && rm -rf .cache && sh run.sh. (Celix Framework will be started, as well as wiring bundles)
8.   Terminal 2: cd deploy/wiring_2 && rm -rf .cache && sh run.sh. (Celix Framework will be started, as well as wiring bundles)
9.   Terminal 1: export (triggers the export of a service)
10.  Terminal 2: send <wireId> <msg> (sends a message to the service in Terminal 1)

