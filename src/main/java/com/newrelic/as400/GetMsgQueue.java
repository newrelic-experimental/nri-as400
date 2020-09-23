//  ----------------------------------------------------------------------
//
//  Name: GetMsgQueue                          Author: Michael Jobe
//                                                     mjobe@newrelic.com
//  Execution Notes
//  Input: GetMsgQueue <host> <userid> <passwd> <queue>
//  Output: All queue messages.  If this is the first execution only
//          the NEWEST message is read from the argumented queue
//          if this is not the first execution the queue is read
//          starting at the message identified by the message key
//          at the last execution.
//
//          This is designed to function as part of a New Relic
//          on host integration.
//
//          Checkpoint files are written to the local directory,
//          each filename will match a argumented host.
//
//  ----------------------------------------------------------------------
//  Modification Log
//  ----------------------------------------------------------------------
//  1/30/2019 - Initial creation - MJ
//  ----------------------------------------------------------------------
package com.newrelic.as400;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.nio.file.Files;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.MessageQueue;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.QueuedMessage;

public class GetMsgQueue {

	@SuppressWarnings("rawtypes")
	public static void main(String[] args) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
		String strInstance = System.getenv("INSTANCE");
		String strAs400 = System.getenv("AS400HOST");
		String strQueue = System.getenv("MSGQUEUE");
		String strUser = System.getenv("USERID");
		String strPass = System.getenv("PASSWD");
		String strNrName = "com.newrelic.as400-message-queue";
		String strNrEventType = "AS400:MessageQueueEvent";
		String strNrProtoVersion = "1";
		String strNrIntVersion = "0.1.0";
		String strJSONMetrics = "";
		String strJSONHeader = ("{" + "\"name\":" + '"' + strNrName + '"' + "," + "\"protocol_version\":" + '"' + strNrProtoVersion + '"' + "," + "\"integration_version\":" + '"' + strNrIntVersion + '"' + "," + "\"metrics\":" + "[");
		String strJSONFooter = ("]," + "\"inventory\":" + "{" + "}," + "\"events\":" + "[" + "]" + "}");
		boolean bFirstRun = true;
		String strNrEventSummary = "AS400 message queue messages";
		AS400 as400 = new AS400(strAs400, strUser, strPass);
//      System.out.println(as400);
//  ----------------------------------------------------------------------
//      Check and see if this is our first run.  If it is not, there will be a checkpoint file
//      containing the message key array in which we need to set our read point.
//  ----------------------------------------------------------------------
		File chkPoint = new File(strInstance);
		if (chkPoint.exists()) {
			bFirstRun = false;
//  ----------------------------------------------------------------------
//      Need to put the code in to read the byte array from the checkpoint file and set
//      mqueue.setUserStartingMessageKey to the read array.
//
//      If bFirstRun is true, we will read in the NEWEST record and start our read
//      from the last message.
//  ----------------------------------------------------------------------
		}
		MessageQueue mqueue = new MessageQueue(as400, strQueue);
//  ----------------------------------------------------------------------
//      If the is our first run, start at NEWEST, otherwise start at msgKey
//      loaded from checkpoint file.
//  ----------------------------------------------------------------------
		if (bFirstRun) {
			mqueue.setUserStartingMessageKey(MessageQueue.NEWEST);
		} else {
			byte[] byteStartKey = Files.readAllBytes(new File(strInstance).toPath());
			mqueue.setUserStartingMessageKey(byteStartKey);
		}
		Enumeration enuma = mqueue.getMessages();
		// System.out.println("Only one element... ");
		while (enuma.hasMoreElements()) {
			QueuedMessage qMsg = (QueuedMessage) enuma.nextElement();
			byte byteMsgKey[] = qMsg.getKey();
			qMsg.getDate();
			if (bFirstRun == true) {
				strJSONMetrics = strJSONMetrics +
						"{" +
							"\"event_type\":" +
							'"' +
							strNrEventType +
							'"' +
							"," +
							"\"summary\":" +
							'"' +
							strNrEventSummary +
							'"' +
							"," +
							"\"host\":" +
							'"' +
							strAs400 +
							'"' +
							"," +
							"\"queue\":" +
							'"' +
							strQueue +
							'"' +
							"," +
							"\"messageID\":" +
							'"' +
							qMsg.getID() +
							'"' +
							"," +
							"\"job\":" +
							'"' +
							qMsg.getFromJobName() +
							'"' +
							"," +
							"\"jobNumber\":" +
							'"' +
							qMsg.getFromJobNumber() +
							'"' +
							"," +
							"\"type\":" +
							'"' +
							qMsg.getType() +
							'"' +
							"," +
							"\"program\":" +
							'"' +
							qMsg.getFromProgram() +
							'"' +
							"," +
							"\"severity\":" +
							qMsg.getSeverity() +
							"," +
							"\"replyStatus\":" +
							'"' +
							qMsg.getReplyStatus() +
							'"' +
							"," +
							"\"user\":" +
							'"' +
							qMsg.getUser() +
							'"' +
							"," +
							"\"message\":" +
							'"' +
							qMsg.getText() +
							'"' +
							"," +
							"\"messageHelp\":" +
							'"' +
							qMsg.getMessageHelp() +
							'"' +
							"},";
				OutputStream byteStream = new FileOutputStream(strInstance);
				for (byte bKey : byteMsgKey) {
					byteStream.write(bKey);
				}
				byteStream.close();
			} else {
				bFirstRun = true;
			}
		}
		strJSONMetrics = strJSONMetrics.substring(0, strJSONMetrics.length() - 1);
		System.out.println(strJSONHeader + strJSONMetrics + strJSONFooter);
	}
}
