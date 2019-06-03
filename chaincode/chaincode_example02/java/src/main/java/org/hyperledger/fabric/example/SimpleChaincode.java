package org.hyperledger.fabric.example;

/*

Copyright IBM Corp., DTCC All Rights Reserved.



SPDX-License-Identifier: Apache-2.0

*/

//package org.hyperledger.fabric.example;



import java.util.List;



import java.io.FileOutputStream;

import java.io.IOException;

import java.io.OutputStream;



import com.google.protobuf.ByteString;

import io.netty.handler.ssl.OpenSsl;

import org.apache.commons.logging.Log;

import org.apache.commons.logging.LogFactory;

import org.hyperledger.fabric.shim.ChaincodeBase;

import org.hyperledger.fabric.shim.ChaincodeStub;



import org.csource.common.NameValuePair;

import org.csource.fastdfs.ClientGlobal;

import org.csource.fastdfs.FileInfo;

import org.csource.fastdfs.StorageClient;

import org.csource.fastdfs.StorageServer;

import org.csource.fastdfs.TrackerClient;

import org.csource.fastdfs.TrackerServer;



import static java.nio.charset.StandardCharsets.UTF_8;



public class SimpleChaincode extends ChaincodeBase {



    private static Log _logger = LogFactory.getLog(SimpleChaincode.class);

	

	public static String conf_filename = "/root/chaincode-java/fdfs_client.conf";



    @Override

    public Response init(ChaincodeStub stub) {

        try {

            _logger.info("Init java simple chaincode");

            String func = stub.getFunction();

            if (!func.equals("init")) {

                return newErrorResponse("function other than init is not supported");

            }

            List<String> args = stub.getParameters();

            if (args.size() != 4) {

                newErrorResponse("Incorrect number of arguments. Expecting 4");

            }

            // Initialize the chaincode

            String account1Key = args.get(0);

            int account1Value = Integer.parseInt(args.get(1));

            String account2Key = args.get(2);

            int account2Value = Integer.parseInt(args.get(3));



            _logger.info(String.format("account %s, value = %s; account %s, value %s", account1Key, account1Value, account2Key, account2Value));

            stub.putStringState(account1Key, args.get(1));

            stub.putStringState(account2Key, args.get(3));



            return newSuccessResponse();

        } catch (Throwable e) {

            return newErrorResponse(e);

        }

    }



    @Override

    public Response invoke(ChaincodeStub stub) {

        try {

            _logger.info("Invoke java simple chaincode");

            String func = stub.getFunction();

            List<String> params = stub.getParameters();

            if (func.equals("invoke")) {

                return invoke(stub, params);

            }

            if (func.equals("delete")) {

                return delete(stub, params);

            }

            if (func.equals("query")) {

                return query(stub, params);

            }

            if (func.equals("fastDFsUploadFile")) {

		System.out.println("Upload sucessful");

		return fastDFsUploadFile(stub, params);

			

	    }

	    if (func.equals("fastDFsDownloadFile")) {

		System.out.println("download sucessful");

		return fastDFsDownloadFile(stub, params);

	    }

	    if (func.equals("fastDFsDeleteFile")) {

		System.out.println("delete sucessful");

		return fastDFsDeleteFile(stub, params);

            }

	    return newErrorResponse("Invalid invoke function name. Expecting one of: [\"invoke\", \"delete\", \"query\",\"fastDFsUploadFile\",\"fastDFsDownloadFile\",\"fastDFsDeleteFile\"]");

        } catch (Throwable e) {

            return newErrorResponse(e);

        }

    }



    private Response invoke(ChaincodeStub stub, List<String> args) {

        if (args.size() != 3) {

            return newErrorResponse("Incorrect number of arguments. Expecting 3");

        }

        String accountFromKey = args.get(0);

        String accountToKey = args.get(1);



        String accountFromValueStr = stub.getStringState(accountFromKey);

        if (accountFromValueStr == null) {

            return newErrorResponse(String.format("Entity %s not found", accountFromKey));

        }

        int accountFromValue = Integer.parseInt(accountFromValueStr);



        String accountToValueStr = stub.getStringState(accountToKey);

        if (accountToValueStr == null) {

            return newErrorResponse(String.format("Entity %s not found", accountToKey));

        }

        int accountToValue = Integer.parseInt(accountToValueStr);



        int amount = Integer.parseInt(args.get(2));



        if (amount > accountFromValue) {

            return newErrorResponse(String.format("not enough money in account %s", accountFromKey));

        }



        accountFromValue -= amount;

        accountToValue += amount;



        _logger.info(String.format("new value of A: %s", accountFromValue));

        _logger.info(String.format("new value of B: %s", accountToValue));



        stub.putStringState(accountFromKey, Integer.toString(accountFromValue));

        stub.putStringState(accountToKey, Integer.toString(accountToValue));



        _logger.info("Transfer complete");



        return newSuccessResponse("invoke finished successfully", ByteString.copyFrom(accountFromKey + ": " + accountFromValue + " " + accountToKey + ": " + accountToValue, UTF_8).toByteArray());

    }



	private Response fastDFsUploadFile(ChaincodeStub stub, List<String> args) { //上传文件

		if (args.size() != 2) {

			return newErrorResponse("Incorrect number of arguments. Expecting 2");

		}

				

		String local_filename = args.get(0);

		String local_filetype = args.get(1);

		String fileIds[] = new String[2];

		String realfilepath = null;



		TrackerServer trackerServer = null;

		StorageServer storageServer = null;

		try {

			ClientGlobal.init(conf_filename);

			TrackerClient tracker = new TrackerClient();

			trackerServer = tracker.getConnection();

			StorageClient storageClient = new StorageClient(trackerServer, storageServer);

			fileIds = storageClient.upload_file(local_filename, local_filetype, null);

			if (fileIds == null) {

				return newErrorResponse("DFsUploadFile is failed");

			}

					

			realfilepath = fileIds[0] + "/" + fileIds[1];

		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			try {

				if (storageServer != null)

					storageServer.close();

				// order

				if (trackerServer != null)

					trackerServer.close();

			} catch (IOException e) {

				e.printStackTrace();

			}

		}

		//System.out.println(realfilepath);

		return newSuccessResponse("uploadfile path:" + realfilepath);

	}



	private Response fastDFsDownloadFile(ChaincodeStub stub, List<String> args) { // 下载文件

		if (args.size() != 3) {

			return newErrorResponse("Incorrect number of arguments. Expecting 3");

		}

		

		

		TrackerServer trackerServer = null;

		StorageServer storageServer = null;



		String groupId = args.get(0);

		String filepath = args.get(1);

		String storePath = args.get(2);

		try {

			ClientGlobal.init(conf_filename);

			TrackerClient tracker = new TrackerClient();

			trackerServer = tracker.getConnection();



			StorageClient storageClient = new StorageClient(trackerServer, storageServer);

			byte[] bytes = storageClient.download_file(groupId, filepath);

			 

			if (bytes == null) {

				newErrorResponse("DownloadFile is failed");

			}



			OutputStream out = new FileOutputStream(storePath);

			out.write(bytes);

			out.close();

		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			try {

				if (storageServer != null)

					storageServer.close();

				if (trackerServer != null)

					trackerServer.close();

			} catch (IOException e) {

				// TODO Auto-generated catch block

				e.printStackTrace();

			} catch (Exception e) {

				e.printStackTrace();

			}

		}

		return newSuccessResponse("download file sucessful!");

	}



	public static Response fastDFsDeleteFile(ChaincodeStub stub, List<String> args) { // 删除文件

		if (args.size() != 2) {

			return newErrorResponse("Incorrect number of arguments. Expecting 2");

		}



		TrackerServer trackerServer = null;

		StorageServer storageServer = null;



		String groupId = args.get(0);

		String Filepath = args.get(1);

		Response deletemessage=newSuccessResponse("delete file failed");



		int i = 0;

		try {

			ClientGlobal.init(conf_filename);

			TrackerClient tracker = new TrackerClient();

			trackerServer = tracker.getConnection();



			StorageClient storageClient = new StorageClient(trackerServer, storageServer);

			i = storageClient.delete_file(groupId, Filepath);



		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			try {

				if (storageServer != null )

					storageServer.close();

				if (trackerServer != null )

					trackerServer.close();

			} catch (IOException e) {

				// TODO Auto-generated catch block

				e.printStackTrace();

			}

		}

		if(i == 0) {

			deletemessage = newSuccessResponse("delete file sucessful!");

		}



		return deletemessage;



	}





	/* public static void testDownload(String path, String storePath) {	//下载文件

		TrackerServer trackerServer =null;

		StorageServer storageServer = null;

		

		String[] downfilepath = path.split("/" , 2);

		String groupId = downfilepath[0];

		String filepath = downfilepath[1];

	    try {

	        ClientGlobal.init(conf_filename);

	        TrackerClient tracker = new TrackerClient(); 

	        trackerServer = tracker.getConnection(); 



	        StorageClient storageClient = new StorageClient(trackerServer, storageServer); 

	        

	        byte[] bytes = storageClient.download_file(groupId, filepath); 

	        

	        OutputStream out = new FileOutputStream(storePath);

	        out.write(bytes);

	        out.close();

	    } catch (Exception e) { 

	        e.printStackTrace(); 

	    } finally {

			try {

				if(null!=storageServer) storageServer.close();

				if(null!=trackerServer) trackerServer.close();

			} catch (IOException e) {

				// TODO Auto-generated catch block

				e.printStackTrace();

			} catch (Exception e) { 

	            e.printStackTrace(); 

	        } 

	    }

	}

	*/

	

    // Deletes an entity from state

    private Response delete(ChaincodeStub stub, List<String> args) {

        if (args.size() != 1) {

            return newErrorResponse("Incorrect number of arguments. Expecting 1");

        }

        String key = args.get(0);

        // Delete the key from the state in ledger

        stub.delState(key);

        return newSuccessResponse();

    }



    // query callback representing the query of a chaincode

    private Response query(ChaincodeStub stub, List<String> args) {

        if (args.size() != 1) {

            return newErrorResponse("Incorrect number of arguments. Expecting name of the person to query");

        }

        String key = args.get(0);

        //byte[] stateBytes

        String val	= stub.getStringState(key);

        if (val == null) {

            return newErrorResponse(String.format("Error: state for %s is null", key));

        }

        _logger.info(String.format("Query Response:\nName: %s, Amount: %s\n", key, val));

        return newSuccessResponse(val, ByteString.copyFrom(val, UTF_8).toByteArray());

    }



    public static void main(String[] args) {

        System.out.println("OpenSSL avaliable: " + OpenSsl.isAvailable());

        new SimpleChaincode().start(args);

    }



}

