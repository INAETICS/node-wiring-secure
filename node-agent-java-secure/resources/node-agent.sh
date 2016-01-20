#!/bin/bash
#
# Start scrip for the Node Agent
#
# (C) 2014 INAETICS, <www.inaetics.org> - Apache License v2.

cd $(dirname $0)

#
# Config
#
PROVISIONING_NAMESPACE="/inaetics/node-provisioning-service"
MAX_RETRY_ETCD_REPO=10
RETRY_ETCD_REPO_INTERVAL=5
UPDATE_INTERVAL=10
RETRY_INTERVAL=5
ETCD_TTL_INTERVALL=$((UPDATE_INTERVAL + 5))
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
    return 0
  fi
  return 1
}

start_agent () {
  local props1="-Dagent.identification.agentid=$agent_id \
    -Dagent.discovery.serverurls=http://$current_provisioning_service \
    -Dagent.controller.syncinterval=10 \
    -Dorg.osgi.service.http.port=$agent_port \
    -Damdatu.remote.logging.level=5 \
    -Damdatu.remote.console.level=5 \
	-Dorg.apache.felix.http.host=$agent_ipv4 \
    -Dorg.amdatu.remote.discovery.etcd.connecturl=http://$ETCDCTL_PEERS \
    -Dorg.amdatu.remote.discovery.etcd.rootpath=/inaetics/discovery \
    -Dorg.amdatu.remote.discovery.etcd.schedule=2 \
    -Dorg.amdatu.remote.admin.http.host=$agent_ipv4 \
    -Dinaetics.wiring.logging.level=5 \
    -Dinaetics.wiring.console.level=5 \
	-Dorg.inaetics.wiring.discovery.etcd.zone=zone1 \
	-Dorg.inaetics.wiring.discovery.etcd.node=$agent_id \
	-Dorg.inaetics.wiring.discovery.etcd.connecturl=http://$ETCDCTL_PEERS \
	-Dorg.inaetics.wiring.discovery.etcd.rootpath=/inaetics/wiring \
	-Dorg.inaetics.wiring.admin.http.zone=zone1 \
	-Dorg.inaetics.wiring.admin.http.node=$agent_id \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"
    
  local props2=-Dgosh.args="--nointeractive --command telnetd --ip=0.0.0.0 start"

  _dbg $props1 "$props2"
  java $props1 "$props2" -jar target.jar &
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
    kill -SIGTERM $agent_pid
    agent_pid=""
    sleep 3
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

  # we are not healthy anymore, when agent_pid is set but process is not running
  if [ "$agent_pid" != "" ] && [ ! -d "/proc/$agent_pid" ] ; then
    # clean up and exit loop
    echo "agent process not running anymore, cleaning up..."
    clean_up
    break
  fi
  
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
