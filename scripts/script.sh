#!/bin/bash

echo
echo " ____    _____      _      ____    _____ "
echo "/ ___|  |_   _|    / \    |  _ \  |_   _|"
echo "\___ \    | |     / _ \   | |_) |   | |  "
echo " ___) |   | |    / ___ \  |  _ <    | |  "
echo "|____/    |_|   /_/   \_\ |_| \_\   |_|  "
echo
echo "Building your network ......"
echo

export ORERER_ADDRESS=orderer.testnet.com:7050
export CHANNEL_NAME=testchannel

# create channel
function createChannel(){
    set -x
    peer channel create -o ${ORERER_ADDRESS} -c ${CHANNEL_NAME} -f /opt/gopath/src/github.com/hyperledger/fabric/peer/channel-artifacts/testchannel.tx >& log.txt 
    res=$?
    set +x
    if [ $res -ne 0 ]; then
        echo "===========$res============="
        echo " ERROR !!! FAILED to create channel"
        exit 1
    fi
}

# join channel 
function joinChannel(){
    set -x
    peer channel join -b testchannel.block >& log.txt
    res=$?
    set +x
    if [ $res -ne 0 ]; then
        echo "===========$res============="
        echo " ERROR !!! FAILED to join channel"
        exit 1
    fi
}

# update anchor peer
function updateAnchor(){
    set -x
    peer channel update -o ${ORERER_ADDRESS} -c ${CHANNEL_NAME} -f /opt/gopath/src/github.com/hyperledger/fabric/peer/channel-artifacts/OrgAanchors.tx >& log.txt 
    res=$?
    set +x
    if [ $res -ne 0 ]; then
        echo "===========$res============="
        echo " ERROR !!! FAILED to update anchor peer"
        exit 1
    fi
}

# install chaincode 
function installCC(){
    set -x
    peer chaincode install -l java -n javaCC -v 0 -p /opt/gopath/src/github.com/chaincode/chaincode_example02/java/ >& log.txt 
    res=$?
    set +x
    if [ $res -ne 0 ]; then
        echo "===========$res============="
        echo " ERROR !!! FAILED to install chaincode"
        exit 1
    fi
}
 
# instantiated chaincode 
function initCC(){
    set -x
    peer chaincode instantiate -o ${ORERER_ADDRESS} -C $CHANNEL_NAME -n javaCC -v 0 -c '{"Args":["init","a","100","b","200"]}' -P "OR('OrgA.peer')" >& log.txt 
    res=$?
    set +x
    sleep 10
    if [ $res -ne 0 ]; then
        echo "===========$res============="
        echo " ERROR !!! FAILED to instantiate chaincode"
        exit 1
    fi
}

# invoke chaincode
function invokeCC(){
    set -x
    peer chaincode invoke -o ${ORERER_ADDRESS} -C $CHANNEL_NAME -n javaCC --peerAddresses peer0.orga.testnet.com:7051 -c '{"Args":["invoke","a","b","10"]}' >& log.txt
    res=$?
    set +x
    if [ $res -ne 0 ]; then
        echo "===========$res============="
        echo " ERROR !!! FAILED to invoke chaincode"
        # exit 1
    fi
}

# query chaincode
function queryCC(){
    set -x
    peer chaincode query -o ${ORERER_ADDRESS} -C $CHANNEL_NAME -n javaCC -c '{"Args":["query", "a"]}' >& log.txt
    res=$?
    set +x
    cat log.txt   
    if [ $res -ne 0 ]; then
        echo "===========$res============="
        echo " ERROR !!! FAILED to query chaincode"
        exit 1
    fi
}


# The first peer create channel
createChannel;
joinChannel
updateAnchor
installCC
initCC
invokeCC
queryCC

echo
echo "========= All GOOD, network built successfully=========== "
echo

echo
echo " _____   _   _   ____   "
echo "| ____| | \ | | |  _ \  "
echo "|  _|   |  \| | | | | | "
echo "| |___  | |\  | | |_| | "
echo "|_____| |_| \_| |____/  "
echo
