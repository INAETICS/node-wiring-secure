#!/bin/bash
#
# Start scrip for the Node Agent
#
# (C) 2014 INAETICS, <www.inaetics.org> - Apache License v2.

#TODO the workdir should be removed when stopping the agent

cd $(dirname $0)

#
# Config
#
PROVISIONING_NAMESPACE="/inaetics/node-provisioning-service"
MAX_RETRY_ETCD_REPO=10
RETRY_ETCD_REPO_INTERVAL=5
UPDATE_INTERVAL=60
RETRY_INTERVAL=20
ETCD_TTL_INTERVALL=$((UPDATE_INTERVAL + 15))
LOG_DEBUG=true

#
# Libs
#
source etcdctl.sh

# Wraps a function call to redirect or filter stdout/stderr
# depending on the debug setting
#   args: $@ - the wrapped call
#   return: the wrapped call's return
_call () {
  if [ "$LOG_DEBUG" != "true"  ]; then
    $@ &> /dev/null
    return $?
  else
    $@ 2>&1 | awk '{print "[DEBUG] "$0}' >&2
    return ${PIPESTATUS[0]}
  fi
}

# Echo a debug message to stderr, perpending each line
# with a debug prefix.
#   args: $@ - the echo args
_dbg() {
  if [ "$LOG_DEBUG" == "true" ]; then
    echo $@ | awk '{print "[DEBUG] "$0}' >&2
  fi
}

# Echo a log message to stderr, perpending each line
# with a info prefix.
#   args: $@ - the echo args
_log() {
  echo $@ | awk '{print "[INFO] "$0}' >&2
}


#
# State
#
current_provisioning_service=""
located_provisioning_service=""
agent_pid=""

#
# Functions
#

# Locate the provisioning service in etcd.
#  args: $1 - <current service>, prefer if present
#  echo: <new service>, may be same as current
#  return: 0, if no errors
#    1, if etcd lookup fails
locate_provisioning_service () {
  located_provisioning_service=""

  local provisioning_services=($(etcd/values $PROVISIONING_NAMESPACE $ETCD_HOST))
  if [ $? -ne 0 ]; then
    return 1
  fi
  if [ "$current_provisioning_service" != "" ]; then
    for provisioning_service in ${provisioning_services[@]}; do
      if [ "$current_provisioning_service" == "$provisioning_service" ]; then
        located_provisioning_service=$current_provisioning_service
        return 0
      fi
    done
  fi
  if [ ${#provisioning_services[@]} -gt 0 ]; then
    located_provisioning_service=${provisioning_services[0]}
  fi
  return 0
}


#if a dir /var/bundles exists. add all bundles (.zip) to the autostart
setup_standalone() {
    props=$1
    
    if [ -d /var/standalone ] ; then
        initial=`cat ${props} | grep "cosgi.auto.start.1="`
        echo -n ${initial} >> ${props}
        for bundle in `ls -1 /var/standalone/bundles` ; do
            echo -n " /var/bundles/${bundle}" >> ${props}
        done
        echo "" >> ${props}
    else 
        echo "Error. Exptected a /var/standalone dir for a standalone cagent!"
        exit 1
    fi

}


start_agent () {
  DEPLOYMENT_ID=${agent_id}
  HOST_IP=${agent_ipv4}
  MAX_RETRY_ETCD_REPO=60
  RETRY_ETCD_REPO_INTERVAL=5
  DISCOVERY_PATH="org.apache.celix.discovery.etcd"

  local workdir="/tmp/workdir"
  mkdir -p ${workdir}

  cp /tmp/config.properties.base ${workdir}/config.properties
  DISCOVERY_ETCD_SERVER_IP=`echo $ETCDCTL_PEERS | cut -d ':' -f 1`
  DISCOVERY_ETCD_SERVER_PORT=`echo $ETCDCTL_PEERS | cut -d ':' -f 2`
 
  echo "deployment_admin_identification=${agent_id}" >> ${workdir}/config.properties
  echo "deployment_admin_url=${current_provisioning_service}" >> ${workdir}/config.properties
  echo "RSA_IP=$agent_ipv4" >> ${workdir}/config.properties
  echo "DISCOVERY_ETCD_TTL=60" >> ${workdir}/config.properties
  echo "DISCOVERY_ETCD_ROOT_PATH=inaetics/discovery" >> ${workdir}/config.properties
  echo "DISCOVERY_ETCD_SERVER_IP=`echo $DISCOVERY_ETCD_SERVER_IP`" >> ${workdir}/config.properties
  echo "DISCOVERY_ETCD_SERVER_PORT=`echo $DISCOVERY_ETCD_SERVER_PORT`" >> ${workdir}/config.properties
  echo "DISCOVERY_CFG_SERVER_IP=$agent_ipv4" >> ${workdir}/config.properties
  echo "LOGHELPER_ENABLE_STDOUT_FALLBACK=true" >> ${workdir}/config.properties
  echo "NODE_DISCOVERY_ZONE_IDENTIFIER=zone1" >> ${workdir}/config.properties
  echo "NODE_DISCOVERY_ETCD_SERVER_IP=`echo $DISCOVERY_ETCD_SERVER_IP`" >> ${workdir}/config.properties
  echo "NODE_DISCOVERY_ETCD_SERVER_PORT=`echo $DISCOVERY_ETCD_SERVER_PORT`" >> ${workdir}/config.properties
  echo "NODE_DISCOVERY_ETCD_ROOT_PATH=inaetics/wiring" >> ${workdir}/config.properties
  echo "NODE_DISCOVERY_NODE_WA_ADDRESS=$agent_ipv4" >> ${workdir}/config.properties
  echo "NODE_DISCOVERY_NODE_WA_PORT=8888" >> ${workdir}/config.properties
  
  if [ -d /var/standalone ] ; then
      setup_standalone ${workdir}/config.properties
  fi


  _log "CELIX Configuration"
  _log "================================================="
  _log "RSA IP s		   : $agent_ipv4"
  _log "DISCOVERY_ETCD_SERVER_IP   : $DISCOVERY_ETCD_SERVER_IP"
  _log "DISCOVERY_ETCD_SERVER_PORT : $DISCOVERY_ETCD_SERVER_PORT"

  cd ${workdir}
  local cmd="celix"

  _dbg $cmd
  $cmd &
  agent_pid=$!
}

function store_etcd_data(){

  # check if provisioning is running
  if [ "$agent_pid" == "" ]; then
  	_log "service not running, skipping store_etcd_data"
    return
  fi

  ETCD_PATH_FOUND=0
  RETRY=1
  while [ $RETRY -le $MAX_RETRY_ETCD_REPO ] && [ $ETCD_PATH_FOUND -eq 0 ]
  do
    etcd/putTtl "/inaetics/node-agent-service/$agent_id" "$agent_ipv4:$agent_port" "$ETCD_TTL_INTERVALL"

    if [ $? -ne 0 ]; then
        _log "Tentative $RETRY of storing agent to etcd failed. Retrying..."
        ((RETRY+=1))
        sleep $RETRY_ETCD_REPO_INTERVAL
    else
        _log "Pair </inaetics/node-agent-service/$agent_id,$agent_ipv4:$agent_port> stored in etcd"
        ETCD_PATH_FOUND=1
    fi
  done

  if [ $ETCD_PATH_FOUND -eq 0 ]; then
    _log "Cannot store pair </inaetics/node-agent-service/$agent_id,$agent_ipv4:$agent_port> in etcd"
  fi

}
stop_agent () {
  etcd/rm "/inaetics/node-agent-service/$agent_id"
  if [ "$agent_pid" != "" ]; then
    kill -SIGINT $agent_pid
      wait $agent_pid
      agent_pid=""
      rm -fr /tmp/workdir
  fi
}

clean_up () {
    echo "Running cleanup.."
    stop_agent
    rm /tmp/health
    exit 0
}

#
# Main
#
trap clean_up SIGHUP SIGINT SIGTERM

agent_id=$1
if [ "$agent_id" == "" ]; then
  # get id from env variable set by kubernetes pod config
  agent_id=$AGENT_NAME
  if [ "$agent_id" != "" ]; then
  	# append ip
    agent_id=$agent_id-`hostname -i`
  fi
fi
if [ "$agent_id" == "" ]; then
  # get docker id
  agent_id=`cat /proc/self/cgroup | grep -o  -e "docker-.*.scope" | head -n 1 | sed "s/docker-\(.*\).scope/\\1/"`
fi
if [ "$agent_id" == "" ]; then
  echo "agent_id param required!"
  exit 1
fi

agent_ipv4=$2
if [ "$agent_ipv4" == "" ]; then
  # get IP 
  agent_ipv4=`hostname -i`
fi
if [ "$agent_ipv4" == "" ]; then
  echo "agent_ipv4 param required!"
  exit 1
fi

# get port from env variable set by kubernetes pod config
agent_port=$HOSTPORT
if [ "$agent_port" == "" ]; then
  agent_port=8080
fi


# we are healthy, used by kubernetes
echo ok > /tmp/health


while true; do


  # we are not healthy anymore when agent_pid is set but process is not running
  if [ "$agent_pid" != "" ] && [ ! -d "/proc/$agent_pid" ] && [ ! -e /tmp/disable_healthcheck ]; then
    # clean up and exit loop
    echo "agent process not running anymore, cleaning up..."
    clean_up
    break
  fi

  if [ -d /var/standalone ] ; then
      if [ "$agent_pid" = "" ] ; then
          echo "Starting standalone agent..."
          start_agent
      fi
  else 
      locate_provisioning_service
      if [ $? -ne 0 ]; then
        echo "Locating provisioning services in etcd failed. Keeping current state.." 1>&2
      else
        if [ "$current_provisioning_service" != "$located_provisioning_service" ]; then
          echo "Provisioning service changed: $current_provisioning_service -> $located_provisioning_service"
          current_provisioning_service=$located_provisioning_service

          if [ "$current_provisioning_service" == "" ]; then
            if [ "$agent_pid" != "" ]; then
              echo "Stopping agent.."
              stop_agent
            fi
          else
            if [ "$agent_pid" != "" ]; then
              echo "Restarting agent..."
              stop_agent
              start_agent
            else
              echo "Starting agent..."
              start_agent
            fi
          fi
        fi
      fi
  fi

  if [ "$agent_pid" == "" ]; then
    echo "agent waiting for provisioning service.."
    echo "Will retry in $RETRY_INTERVAL seconds..."
    sleep $RETRY_INTERVAL &
    wait $!
  else
    echo "agent running with provisioning $current_provisioning_service"
    store_etcd_data
    echo "Will update in $UPDATE_INTERVAL seconds..."
    sleep $UPDATE_INTERVAL &
    wait $!
  fi

done
