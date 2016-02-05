# The INAETICS Secure Wiring Implementation
A secured version of the inaetics wiring implementation by using short lived certificates with tls.

## The Certificate Authority
The certificate authority is a (containerized) cloudflare cfssl ca\
[Link to the cfssl project github](https://github.com/cloudflare/cfssl)


## Java implementation
Relevant for the java implementation:
-	[The bundles for the demonstrator cluster - /bundles](bundles)
-	[The node agent to start the felix framework - /node-agent-java-secure](node-agent-java-secure)
-	[The java node wiring - /node-wiring-secure-java](node-wiring-secure-java)

## C implementation
Relevant for the java implementation:
-	The node agent to start the celix framework (TODO)
-	[The c node wiring](node-wiring-secure-c) (only key generation and cert signing request)

