# INAETICS Trust Manager C implementation
This is the C implementation of the INAETICS Trust Manager, which provides functionality for managing short lived trust on nodes in cloud applications within a PKI. This Trust Manager requires [CloudFlare CFSSL] running as Certificate Authority on the network.

## Why Short-Lived Trust
Certificate Revocation Lists (CRLs) and Online Certificate Status Protocol (OCSP) are great technologies for checking the current status of certificates. The problem with these technologies is, that they introduce both scale and management challenges in deployed cloud applications. Short-lived trust enables added security in such environments, since compromised keys are less valuable to an attacker.

## Configuration
The trust manager is implemented as [Apache Celix] Bundle and can be configured using the following properties in as run descriptor.

- **trust.manager.refresh.interval**
    - *Default: 60*
    - Specifies the refresh interval in seconds. A worker runs in the background and continuously checks if the current certificate is still valid. If the certificate is invalid (f.e. expired) a new certificate will be retrieved from the CA.
- **trust.manager.key.storage**
    - *Default: /tmp/inaeticstrustmanager*
    - Specifies the storage location of the keys and certificates.
- **trust.manager.ca.host**
    - *Default: localhost*
    - Specifies the host or ip for the [CloudFlare CFSSL] CA.
- **trust.manager.ca.port**
    - *Default: 8888*
    - Specifies the port for the [CloudFlare CFSSL] CA.

## API
The following api is provided by the trust manager bundle:

### Retrieve current certificates and key file references
> Retrieve an absolute reference to a file containing the latest, valid certificate:
```
extern int get_current_certificate(char* certificate_filepath)
```
___
> Retrieve an absolute reference to a file containing the latest, valid certificate, private and public key:
```
extern int get_current_full_certificate(char* certificate_filepath)
```
___
> Retrieve an absolute reference to a file containing the latest, valid ca certificate:
```
extern int get_current_ca_certificate(char* ca_cert_filepath)
```
___
> Retrieve an absolute reference to a file containing the latest, valid private key, that can be used with the current certificate.
```
extern int get_current_private_key(char* key_filepath)
```
___
> Retrieve an absolute reference to a file containing the latest, valid public key, that can be used with the current certificate.
```
extern int get_current_public_key(char* key_filepath)
```
### Retrieve current certificates and keys as textual representation
> Retrieve a textual representation of the latest, valid certificate:
```
extern int get_current_certificate_content(char* content)
```
___
> Retrieve a textual representation of the latest, valid certificate, private and public key:
```
extern int get_current_full_certificate_content(char* content)
```
___
> Retrieve a textual representation of the latest, valid ca certificate:
```
extern int get_current_ca_certificate_content(char* content)
```
___
> Retrieve a textual representation of the latest, valid private key, that can be used with the current certificate.
```
extern int get_current_private_key_content(char* content)
```
___
> Retrieve a textual representation of the latest, valid public key, that can be used with the current certificate.
```
extern int get_current_public_key_content(char* content)
```

### Version
0.9

### Tech
The INAETICS Trust Manager uses the following libraries and open source projects:

* [mbed TLS] - powerful TLS library with small footprint
* [CloudFlare CFSSL] - open source toolkit for everything TLS/SSL
* [curl] - open source library for transferring data with URL syntax and HTTP/S
* [Apache Celix] - An implementation of the OSGi specification adapted to C

### Todos

 - Lock certificate retrieved on trust refresh
 - Configurable key storage location
 - Configurable CA host & port

License
----
[Apache License Version 2.0, January 2004]

More Information About The INAETICS Project
----
**[INAETICS Project Web Page]**

[//]: # (date: March, 2016 author: INAETICS Project Team, Martin Gaida)

   [CloudFlare CFSSL]: <https://github.com/cloudflare/cfssl>
   [Apache Celix]: <https://celix.apache.org/>
   [Apache License Version 2.0, January 2004]: <https://github.com/INAETICS/Documentation/blob/master/LICENSE>
   [INAETICS Project Web Page]: <http://www.inaetics.org/>
   [Source: Digicert]: <https://blog.digicert.com/short-lived-certificates/>
   [mbed TLS]: <https://tls.mbed.org/>
   [curl]: <https://curl.haxx.se/>
