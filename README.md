# The INAETICS Secure Wiring Implementation
A secured version of the inaetics wiring implementation by using short lived certificates with tls.

## The Certificate Authority
[Link to the sub project](/inaetics-ca)  
The certificate utilizing a (containerized) Cloudflare CFSSL PKI Toolkit and has a REST api.  
This authority has to be deployed in an INAETICS cluster. Afterwards, the (secure) wiring has to be configured to know the host(s) of this certificate authority.  
The configuration of the certificate authority can be modified to specify the time duration in which a certificate is valid.  
[Link to the cfssl project github](https://github.com/cloudflare/cfssl)


## Java implementation

The Java implementation of the project consists of the following sub projects and bundles:
- [Complete Java wiring main project](node-wiring-secure-java)
  - [INAETICS Trust Storage](node-wiring-secure-java/org.inaetics.truststorage)
    - Is deployed on every node and manages all keys and certificates for this nodes within the INAETICS cluster.
  - [INAETICS Certificate Service](node-wiring-secure-java/org.inaetics.certificateservice)
    - Is deployed on every node and communicates with the Certificate Authority and obtains the certificates from the authority. This project has to be changed, once a different signing policy is implemented.

Relevant for the java implementation:
- [The bundles for the demonstrator cluster - /bundles](bundles)
- [The node agent to start the felix framework - /node-agent-java-secure](node-agent-java-secure)
- [The java node wiring - /node-wiring-secure-java](node-wiring-secure-java)

## C implementation

The C implementation of the project consists of the following sub projects and bundles:
- [Complete C wiring main project](node-wiring-secure-c)
  - [INAETICS Trust Manager](node-wiring-secure-c/node-wiring/trust_manager)
    - Is deployed on every node and manages all keys and certificates for this nodes within the INAETICS cluster. Requires a [CloudFlare CFSSL] CA.
    - [Documenatation can be found here](node-wiring-secure-c/node-wiring/trust_manager/README.MD)

Relevant for the C implementation:
- [The bundles for the demonstrator cluster - /bundles](bundles)
- [The node agent to start the celix framework - /node-agent-java-secure](node-agent-c-secure)
- [The (secure) c node wiring - /node-wiring-secure-c](node-wiring-secure-c)

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
   [INAETICS Celix Node Agent]: <https://github.com/INAETICS/node-agent-c>

