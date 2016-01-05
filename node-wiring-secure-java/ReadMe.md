# Node Wiring and RSA INAETICS [![Build Status](https://travis-ci.org/INAETICS/node-wiring-java.svg?branch=master)](https://travis-ci.org/INAETICS/node-wiring-java)

## Build

Build all bundles by executing `./gradlew jar` in the root directory, artifacts will be put to the "generated" folder of each project. 

## Node Wiring

This a HTTP based implementation of the INAETICS Wiring, see https://inaetics.atlassian.net/wiki/display/IN/Wiring+Logic

Main project: org.inaetics.wiring  
Demo project: org.inaetcis.wiring.demo

Using the demo project:

- change runproperties in echoService.bndrun and echoClient.bndrun to your environment
- start echoService.bndrun
- start echoClient.bndrun
- find the wireId created by the echoService in etcd
- on the Gogo shell of the echoClient execute `sendMessage <wireId> <message>`
- result should be "echo: &lt;message&gt;"

## RSA Inaetics

This is a Remote Service Admin implementation based on INAETICS Wiring

Main project: org.inaetics.remote  
iTests: org.inaetics.remote.itest  
Demo project: org.inaetics.remote.demo

Using the demo project:

- change runproperties in simpleEchoService.bndrun and echoClient.bndrun to your environment
- start simpleEchoService.bndrun
- start echoClient.bndrun
- on the Gogo shell of the echoClient execute `sendMessage <message>`
- result should be "echo: &lt;message&gt;"
