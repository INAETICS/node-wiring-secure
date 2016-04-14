#!/bin/bash
rm -rf node-agent-c
git clone https://github.com/INAETICS/node-agent-c --recursive
/bin/cp -f resources/node-agent.sh node-agent-c/buildroot_minimum_celix/inaetics/resources-celix/node-agent.sh
echo "An updated version of the celix node agent can be found in the folder ./node-agent-c"
echo "Please also have a look at: https://github.com/INAETICS/node-agent-c"
