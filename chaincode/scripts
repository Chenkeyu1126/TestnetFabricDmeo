# chaincode 安装运行脚本

1. 设置环境变量，peer的编号
```
$ export CORE_PEER_LOCALMSPID=OrgA
```

2. 设置环境变量，peer账号
```
$ export CORE_PEER_MSPCONFIGPATH=$PWD/crypto-config/peerOrganizations/orga.atlchain.com/users/Admin@orga.atlchain.com/msp/
```

3. 安装链码
``` 
$ peer chaincode install -n atlchainCC -v 0.3 -p ATLab-ATLChain
```

4. 实例化链码
``` 
$ peer chaincode instantiate -o 127.0.0.1:7050 -C atlchannel -n atlchainCC -v 0.3 -c '{"Args":["init"]}'
```

5. 升级链码（先install再upgrade，不需要instantiate）
```
$ peer chaincode install -n atlchain -v 0.2 -p ATLab-ATLChain
$ peer chaincode upgrade -o 127.0.0.1:7050 -C atlchannel -n atlchain -v 0.2 -c '{"Args":["init"]}'
```

6. 写入一条记录数据，key => addB
```
// 如果不指定背书节点IP，则根据配置文件查询
// atlchainCC-v0.1
$ peer chaincode invoke -o 127.0.0.1:7050 -C atlchannel -n atlchain -c '{"Args":["putRecord", "addB", "addA", "100", "20181107", "hashcode"]}'

// atlchainCC-v0.4.1
$ peer chaincode invoke -o 127.0.0.1:7050 -C atlchannel -n atlchainCC -c '{"Args":["Put", "tryPutkey", "{\"tryAddrReceive\":\"trytestAddrA\", \"tryAddrSend\":\"trytestAddrB\"}", "trysignagure", "trypubKey"]}'
```

7. 根据买方地址查询交易记录
```
// atlchainCC-v0.1
$ peer chaincode query -C atlchannel -n atlchain -c '{"Args":["getRecordByBuyerAddr","addB"]}'

// atlchainCC-v0.4.1
$ peer chaincode query -C atlchannel -n atlchainCC -c '{"Args":["Get", "{\"tryAddrSend\":\"trytestAddrB\"}"]}'

$ peer chaincode query -C atlchannel -n atlchainCC -c '{"Args":["Get", "{\"price\":\"10000\"}"]}'

$ peer chaincode query -C atlchannel -n atlchainCC -c '{"Args":["getRecordByKey", "93f34d06fcf4ce30e2745ec11d856506ef9b"]}'
```

8. 根据买方地址查询历史交易记录
```
$ peer chaincode query -C atlchannel -n atlchain -c '{"Args":["getHistoryByBuyerAddr","addB"]}'
// 结果示例 {"TxId":"3a198be789b60fb964141e4d4e24b47298f874b0378900ae5e33c98f401afbb9", "Value":{"Buyer":"addB","Seller":"addC","Price":100,"Time":"20181108","Hash":"hashcode2"}, "Timestamp":"2018-11-07 06:58:56.420657457 +0000 UTC", "IsDelete":"false"}

// atlchainCC-v0.3 
$ peer chaincode invoke -o 127.0.0.1:7050 -C atlchannel -n atlchainCC -c '{"Args":["getHistoryByKey", "93f34d06fcf4ce30e2745ec11d856506ef9b"]}'
```

9. 根据hash查询交易记录 
$ peer chaincode query -C atlchannel -n atlchain -c '{"Args":["getHistoryByHash","hashcode"]}'

10. 根据hash和买方地址查询交易记录
peer chaincode query -C atlchannel -n atlchain -c '{"Args":["getHistoryByHashAndBuyerAddr","hashcode","addB"]}'

11. 调试模式时用
```
$ export CORE_PEER_ADDRESS=127.0.0.1:7051
$ export CORE_CHAINCODE_ID_NAME=atlchainCC:0.4
$ 改变core.yaml的配置文件将net模式改为dev模式
$ ./transaction -peer.address=127.0.0.1:7052
```

10. 查询peer中已安装和已实例化的链码
```
$ peer chaincode list --instantiated -C atlchannel
$ peer chaincode list --installed
```

11. 打包
```
$ peer chaincode package -n atlchainCC -p ATL -v 0.4 atlchainCC0_4.out
```