# INAETICS CA

This certificate authority manages the trust within a INAETICS cluster.

# First steps
It is really important, that keys have to be generated before using the 

# Configuration
There are a couple things that have to be configured specific to the system. These things can be configured in the `inaetics-ca/ca-config.json` file. Please note, that if you change the port of the CA, you also have to modify the `Dockerfile`.
  - The time that a certificate is valid
    - `backdate` sets the duration for which the cert will be dated back, to prevent overlapping.
      - Except for `default`, multiple profiles can be defined. This can f.e. be used to used very short lifespans of certificates for critical parts of infrastructure and longer lifespans for not important parts. This feature has to be implemented in the "C implementation Trust Manager" and the "Java implementation Certificate Service".

# Important TODO's
In the current version of the CA, the keys are included in to the docker image. This is just intended to make the development process easier and *CAN NOT BE USED IN PRODUCTION*. Please mount a volume or use environment variables for the keys.

### Version
1.0

License
----
[Apache License Version 2.0, January 2004]

More Information About The INAETICS Project
----
**[INAETICS Project Web Page]**

[//]: # (date: April, 2016 author: INAETICS Project Team, Martin Gaida)

   [Apache License Version 2.0, January 2004]: <https://github.com/INAETICS/Documentation/blob/master/LICENSE>
      [INAETICS Project Web Page]: <http://www.inaetics.org/>
