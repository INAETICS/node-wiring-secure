# Secure Node Wiring and RSA INAETICS for Java

## Build

Build all bundles by executing `./gradlew jar` in the root directory, artifacts will be put to the "generated" folder of each project. 

## Node Wiring

This a HTTPS (TLS) based implementation of the INAETICS Wiring, see https://inaetics.atlassian.net/wiki/display/IN/Wiring+Logic

Main project: org.inaetics.wiring  
Demo project: org.inaetcis.remote.demo

The best way to get started with the wiring implementation would be to run the demo project. There are multiple demo projects available, but the best one to test the secure wiring implementation is the "Calculator Example". To run this, you have to run two instances, the calculator client and the calculator server.

Using the calculator demo project:

- change runproperties in the `calculator.\*.bndrun` to your environment
- start `calculator.client.bndrun` (instance #1)
- start `calculator.server.bndrun` (instance #2)
- find the wireId created by the echoService; get the wire address in etcd - `etcdctl ls --recursive /` & `etcdctl get /a/b/c/d`
- on the Gogo shell of the calculator client execute `add {num1} {num2}`
- result should be the sum of both numbers.
