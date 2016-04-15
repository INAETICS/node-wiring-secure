# INAETICS CA

This certificate authority manages the trust within a INAETICS cluster.

# First steps
It is really important to generate a key pair before deploying the CA anywhere in production.  The best way would be to generate a root key pair first, which is kept completely safe in an offline storage.  These keys can be used to generate a intermediate certificate for this certificate authority.

Keys can be generated using this command:
`cfssl genkey -initca /etc/cfssl/ca.json | cfssljson ca`

Read below for details on how to install cfssl locally.

# Configuration
There are a couple things that have to be configured specific to the system. These things can be configured in the `inaetics-ca/ca-config.json` file. Please note, that if you change the port of the CA, you also have to modify the `Dockerfile`.
  - The time that a certificate is valid
  - `backdate` sets the duration for which the cert will be dated back, to prevent overlapping.
  - Except for `default`, multiple profiles can be defined. This can f.e. be used to used very short lifespans of certificates for critical parts of infrastructure and longer lifespans for not important parts. This feature has to be implemented in the "C implementation Trust Manager" and the "Java implementation Certificate Service".

More information on how to configure the CFSSL CA can be found [here](https://github.com/cloudflare/cfssl/blob/master/README.md).

# Important TODO's
In the current version of the CA, the keys are included in to the docker image. This is just intended to make the development process easier and *CAN NOT BE USED IN PRODUCTION*. Please mount a volume or use environment variables for the keys, depending on your orchestration technology. [Example using secrets in Kubernetes.](http://kubernetes.io/docs/user-guide/secrets/)

# Docker Info
If this certificate authority is being run in the demonstrator project, it should be tagged with the following tag.  
`172.17.8.20:5000/inaetics/inaetics-ca`

# CFSSL installation
1. [Install go](https://golang.org/dl/)  
2. Get all cfssl tools to your `$GOPATH`  
```sh
$ go get -u github.com/cloudflare/cfssl/cmd/...
```
Make sure you have `$GOPATH/bin/` in your `$PATH`

Detailed info: https://github.com/cloudflare/cfssl/blob/master/BUILDING.md

### Version
1.0

### License
[Apache License Version 2.0, January 2004]

### More Information About The INAETICS Project
**[INAETICS Project Web Page]**

[//]: # (date: April, 2016 author: INAETICS Project Team, Martin Gaida)

   [Apache License Version 2.0, January 2004]: <https://github.com/INAETICS/Documentation/blob/master/LICENSE>
   [INAETICS Project Web Page]: <http://www.inaetics.org/>
