# Helper functions for working with etcdctl in bash.
#
# (C) 2014 INAETICS, <www.inaetics.org> - Apache License v2.

# List child keys for an etcd key
#  args: $1 - key, etcd key
#    $2 - peers, etcd peers
#  echo: a space seperate list of keys
#  return: etcdctl exit code
etcd/keys () {
  _dbg "-> $FUNCNAME - args: $@"
  local key=$1
  local peers=${2:-$ETCDCTL_PEERS}
  local resp
  if [ -n "$peers" ]; then
    resp=($(etcdctl --peers "$peers" ls $key 2>/dev/null))
  else
    resp=($(etcdctl ls $key 2>/dev/null))
  fi
  local code=$?
  if [ $code -eq 0 ]; then
    echo "${resp[@]}"
    return 0
  else
    _dbg "Failed to get keys for: $key"
    return $code
  fi
}

# Get the value for an etcd key
#  args: $1 - key, etcd key
#    $2 - peers, etcd peers
#  echo: <value>, if success
#  return: etcdctl exit code
etcd/value () {
  _dbg "-> $FUNCNAME - args: $@"
  local key=$1
  local peers=${2:-$ETCDCTL_PEERS}
  local resp
  if [ -n "$peers" ]; then
    resp=$(etcdctl --peers "$peers" get $key 2>/dev/null)
  else
    resp=$(etcdctl get $key 2>/dev/null)
  fi
  local code=$?
  if [ $code -eq 0 ]; then
    echo $resp
    return 0
  else
    _dbg "Failed to get value for: $key"
    return $code
  fi
}

# Lists the child values for an etcd key
#  args: $1 - key, etcd key
#    $2 - peers, etcd peers
#  echo: [[<value>]...], if success
#  return: etcdctl exit code
etcd/values () {
  _dbg "-> $FUNCNAME - args: $@"
  local key=$1
  local peers=${2:-$ETCDCTL_PEERS}
  local keys
  keys=$(etcd/keys $key $peers)
  local code=$?
  if [ $code -gt 0 ] ; then
    _dbg "Failed to get keys for: $key"
    return $code
  fi
  local values=()
  local resp
  for child in ${keys[@]}; do
    resp=$(etcd/value $child $peers)
    if [ $? -eq 0 ]; then
      values=("${values[@]}" $resp)
    fi
  done
  echo "${values[@]}"
}

# Sets the value for an etcd key
#  args: $1 - key, etcd key
#    $2 - value, etcd value
#    $3 - peers, etcd peers
#  echo: [[<value>]...], if success
#  return: etcdctl exit code
etcd/put () {
  _dbg "-> $FUNCNAME - args: $@"
  local key=$1
  local value=$2
  local peers=${3:-$ETCDCTL_PEERS}
  if [ -n "$peers" ]; then
    _call etcdctl --peers "$peers" set "$key" "$value"
  else
    _call etcdctl set "$key" "$value"
  fi
  local code=$?
  if [ $code -ne 0 ]; then
    _dbg "Failed to set value for key: $key $value"
    return $code
  fi
}

# Sets the value for an etcd key
#  args: $1 - key, etcd key
#    $2 - value, etcd value
#    $3 - ttl, etcd ttl
#    $4 - peers, etcd peers
#  echo: [[<value>]...], if success
#  return: etcdctl exit code
etcd/putTtl () {
  _dbg "-> $FUNCNAME - args: $@"
  local key=$1
  local value=$2
  local ttl=$3
  local peers=${4:-$ETCDCTL_PEERS}
  if [ -n "$peers" ]; then
    _call etcdctl --peers "$peers" set "$key" "$value" --ttl "$ttl" 
  else
    _call etcdctl set "$key" "$value" --ttl "$ttl"
  fi
  local code=$?
  if [ $code -ne 0 ]; then
    _dbg "Failed to set value for key: $key $value"
    return $code
  fi
}

# Sets the value for an etcd key
#  args: $1 - key, etcd key
#    $2 - peers, etcd peers
#  echo: [[<value>]...], if success
#  return: etcdctl exit code
etcd/rm () {
  _dbg "-> $FUNCNAME - args: $@"
  local key=$1
  local peers=${2:-$ETCDCTL_PEERS}
  if [ -n "$peers" ]; then
    _call etcdctl --peers "$peers" rm "$key"
  else
    _call etcdctl rm "$key"
  fi
  local code=$?
  if [ $code -ne 0 ]; then
    _dbg "Failed to set value for key: $key $value"
    return $code
  fi
}



###EOF###
