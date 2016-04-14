# Dockerfile for inaetics/node-agent-service
FROM slintes/jre8:java8-8u65

#Based on image from Marc Sluiter <marc.sluiter@luminis.eu>
MAINTAINER Inaetics Project Team <info@inaetics.org>

# Install etcdctl
RUN cd /tmp \
  && export ETCDVERSION=v2.3.1 \
  && curl -k -L https://github.com/coreos/etcd/releases/download/$ETCDVERSION/etcd-$ETCDVERSION-linux-amd64.tar.gz | gunzip | tar xf - \
  && cp etcd-$ETCDVERSION-linux-amd64/etcdctl /bin/

# Node agent resources
ADD resources /tmp
