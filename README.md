# The INAETICS Secure Wiring Implementation
A secured version of the inaetics wiring implementation by using short lived certificates with tls.

## The Certificate Authority
The certificate authority is a (containerized) cloudflare cfssl ca\
[Link to the cfssl project github](https://github.com/cloudflare/cfssl)


## Java implementation
Relevant for the java implementation:
- [The bundles for the demonstrator cluster - /bundles](bundles)
- [The node agent to start the felix framework - /node-agent-java-secure](node-agent-java-secure)
- [The java node wiring - /node-wiring-secure-java](node-wiring-secure-java)

## C implementation

The C implementation of the project consists of the following sub projects and bungles:
- The node agent to start the celix framework (TODO)
- [Complete C wiring main project](node-wiring-secure-c)
  - [INAETICS Trust Manager](node-wiring-secure-c/node-wiring/trust_manager)
    - Is deployed on every node and manages all keys and certificates for this nodes within the INAETICS cluster. Requires a [CloudFlare CFSSL] CA.
    - [Documenatation can be found here](node-wiring-secure-c/node-wiring/trust_manager/README.MD)

License
----
[Apache License Version 2.0, January 2004]

More Information About The INAETICS Project
----
**[INAETICS Project Web Page]**

[//]: # (date: March, 2016 author: INAETICS Project Team, Martin Gaida)

   [CloudFlare CFSSL]: <https://github.com/cloudflare/cfssl>
   [Apache License Version 2.0, January 2004]: <https://github.com/INAETICS/Documentation/blob/master/LICENSE>
   [INAETICS Project Web Page]: <http://www.inaetics.org/>

