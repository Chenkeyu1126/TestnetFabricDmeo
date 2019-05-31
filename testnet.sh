#!/bin/bash

export PATH=${PWD}/bin:$PATH
export FABRIC_CFG_PATH=${PWD}

CHANNEL_NAME="testchannel"

#compose files
DOCKER_COMPOSE_FILE="docker-compose-testnet.yaml"

# default compose project name
export COMPOSE_PROJECT_NAME=testnetproj

export DOCKER_COMPOSE_PEER_ADDRESS=peer0.orga.testnet.com:7051
export DOCKER_COMPOSE_PEER_CC_ADDRESS=peer0.orga.testnet.com:7052
export DOCKER_COMPOSE_PEER_GOSSIP_BOOTSTRAP=peer0.orga.testnet.com:7051 

export CORE_PEER_ADDRESS=peer0.orga.testnet.com:7051 
export ORERER_ADDRESS=orderer.testnet.com:7050

function printHelp() {
    echo "Usage: "
    echo "  testnet.sh <mode> "
    echo "      <mode> - one of 'up', 'down'"
    echo "        - 'up' - bring up the network with docker-compose up"
    echo "        - 'down' - clear the network with docker-compose down"
    echo "e.g."
    echo "  testnet.sh up"
    echo "  testnet.sh down"
}

# Generates Org certs using cryptogen tool
function genCerts() {
    which cryptogen
    if [ "$?" -ne 0 ]; then
        echo "cryptogen tool not found."
        exit 1
    fi
    echo
    echo "##########################################################"
    echo "##### Generate certificates using cryptogen tool #########"
    echo "##########################################################"

    if [ -d "crypto-config" ]; then
        rm -rf crypto-config
    fi
    set -x
    cryptogen generate --config=./crypto-config.yaml
    res=$?
    set +x
    if [ $res -ne 0 ]; then
        echo "Failed to generate certificates..."
        exit 1
    fi
    echo
}

# Generate Channel Artifacts used in the network
function genChannelArtifacts() {
    which configtxgen
    if [ "$?" -ne 0 ]; then
        echo "configtxgen tool not found. exiting"
        exit 1
    fi

    if [ ! -d "./channel-artifacts" ]; then
        mkdir ./channel-artifacts
    fi

    echo "##########################################################"
    echo "#########  Generating Orderer Genesis block ##############"
    echo "##########################################################"
    set -x
    configtxgen -profile OrdererChannel -channelID ordererchannel -outputBlock ./channel-artifacts/genesis.block
    res=$?
    set +x
    if [ $res -ne 0 ]; then
        echo "Failed to generate orderer genesis block..."
        exit 1
    fi

    echo
    echo "#################################################################"
    echo "### Generating channel configuration transaction 'testchannel.tx' ###"
    echo "#################################################################"
    set -x
    configtxgen -profile TxChannel -outputCreateChannelTx ./channel-artifacts/testchannel.tx -channelID $CHANNEL_NAME
    res=$?
    set +x
    if [ $res -ne 0 ]; then
        echo "Failed to generate channel configuration transaction..."
        exit 1
    fi
    
    echo
    echo "#################################################################"
    echo "#######    Generating anchor peer update for Org   ##########"
    echo "#################################################################"
    set -x
    configtxgen -profile TxChannel -outputAnchorPeersUpdate ./channel-artifacts/OrgAanchors.tx -channelID $CHANNEL_NAME -asOrg OrgA
    res=$?
    set +x
    if [ $res -ne 0 ]; then
        echo "Failed to generate anchor peer update for Org..."
        exit 1
    fi
    echo 
}

function startAll() {
    set -x
    genCerts
    genChannelArtifacts
    docker-compose -f ${DOCKER_COMPOSE_FILE} up -d 2>&1
    docker exec -it cli.testnet.com scripts/script.sh
    set +x
    if [ $? -ne 0 ]; then
        echo "ERROR !!!! Unable to start orderer node"
        exit 1
    fi
}

# Remove the files generated
function cleanFiles() {
    set -x
    rm -rf crypto-config
    rm -rf channel-artifacts
    set +x
}

function clearContainers() {
  CONTAINER_IDS=$(docker ps -a | awk '($2 ~ /dev-peer.*.javacc.*/) {print $1}')
  if [ -z "$CONTAINER_IDS" -o "$CONTAINER_IDS" == " " ]; then
    echo "---- No containers available for deletion ----"
  else
    docker rm -f $CONTAINER_IDS
  fi
}

function removeUnwantedImages() {
  DOCKER_IMAGE_IDS=$(docker images | awk '($1 ~ /dev-peer.*.javacc.*/) {print $3}')
  if [ -z "$DOCKER_IMAGE_IDS" -o "$DOCKER_IMAGE_IDS" == " " ]; then
    echo "---- No images available for deletion ----"
  else
    docker rmi -f $DOCKER_IMAGE_IDS
  fi
}

function stopAll() {
    set -x
    docker-compose -f ${DOCKER_COMPOSE_FILE} down 2>&1
    cleanFiles
    clearContainers
    removeUnwantedImages
    sleep 3
    echo "y" | docker volume prune
    echo "y" | docker network prune
    echo ""
    set +x
}

# Network config files in ATLChain_NETWORK directory
if [ ! -d "bin" ] 
then
    echo "extract binary files..."
    tar xvf bin.tar.gz
fi

MODE=$1
shift
NODE=$1
shift
# Determine whether starting or stopping
if [ "$MODE" == "up" ]; then
    startAll
elif [ "$MODE" == "down" ]; then
    stopAll
else
    printHelp
    exit 1 
fi
