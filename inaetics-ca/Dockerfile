FROM golang:1.6

ENV USER root

# Install pkcs11 deps
RUN apt-get update && apt-get install -y \
	libltdl-dev \
	--no-install-recommends \
	&& rm -rf /var/lib/apt/lists/*

MAINTAINER Inaetics Project Team Secure Wiring

RUN go get -u github.com/cloudflare/cfssl/cmd/...

WORKDIR /etc/cfssl

# copy inaetics ca test certs and keys
ADD inaetics-certs /etc/cfssl

# currently, the certs are the test certs & keys, these should be generated in upcoming versions.
# RUN cfssl genkey -initca /etc/cfssl/ca.json | cfssljson ca

# expose and run
EXPOSE 8888
ENTRYPOINT ["cfssl"]
#CMD ["serve", "-adress=-0.0.0.0"]
CMD ["serve", "-address=0.0.0.0", "-ca=ca.pem", "-ca-key=ca-key.pem", "-config=ca-config.json"]
