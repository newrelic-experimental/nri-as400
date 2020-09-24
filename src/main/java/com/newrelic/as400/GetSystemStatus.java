//  ----------------------------------------------------------------------
//
//  Name: GetSystemStatus
//
//  ----------------------------------------------------------------------
//  Modification Log
//  ----------------------------------------------------------------------
//  2/9/2019 - Initial creation - MJ
//  9/2/2020 - Capture CPU% from AS400 QWCRSSTS API  - EK
//  ----------------------------------------------------------------------
package com.newrelic.as400;

import java.io.*;
import java.util.*;
import com.ibm.as400.access.*;

public class GetSystemStatus {
	
	private static  ProgramCall s_getSystemStatus = null;
	private static boolean s_getSystemStatus_Result = false;
	
	// SSTS0100 Format fields
	private static Date s_dateTimeStatusGathered;							// offset:   8 
	private static int s_currentUsersSignedOn;								// offset: 	24
	private static int s_usersSuspendedBySystemRequest;						// offset:  32
	private static int s_usersSignedOffWithPrinterOutputWaitingToPrint;		// offset:  40
	private static int s_numberOfBatchJobsWaitingForMessage;				// offset:  44
	private static int s_numberOfBatchJobsRunning;							// offset:  48
	private static int s_numberOfBatchJobsEnding;							// offset:  56
	private static int s_numberOfBatchJobsWaitingToRunOrAlreadyScheduled;	// offset:  60
	private static int s_numberOfBatchJobsHeldOnQueue;						// offset:  64
	private static int s_numberOfBatchJobsOnUnassignedQueues;				// offset:  72
	private static int s_batchJobsEndedWithPrinterOutputWaitingToPrint;		// offset:  76


	// SSTS0200 Format fields
	private static String s_systemName;										// offset:  16
	private static boolean s_restrictedStateFlag;							// offset:  30
	private static float s_percentProcessingUnitUsed;						// offset:  32
	private static int s_numberOfJobsInSystem;								// offset:  36
	private static float s_percentTemporaryAddresses;						// offset:  44
	private static int s_systemASP;											// offset:  48
	private static float s_percentSystemASPUsed;							// offset:  52
	private static int s_totalAuxiliaryStorage;								// offset:  56
	private static int s_currentUnprotectedStorageUsed;						// offset:  60
	private static int s_maxUnprotectedStorageUsed;							// offset:  64
	private static float s_percentDBCapability;								// offset:  68
	private static long s_mainStorageSize;									// offset:  72
	private static int s_numberOfPartitions;								// offset:  76
	private static int s_partitionIdentifier;								// offset:  80
	private static float s_currentProcessingCapacity;						// offset:  88
	private static int s_processorSharingAttribute;							// offset:  92
	private static int s_numberOfProcessors;								// offset:  96
	private static int s_numberActiveJobsInSystem;							// offset: 100
	private static int s_activeThreadsInSystem;								// offset: 104
	private static long s_maxJobsInSystem;									// offset: 108
	private static float s_percentTemporary256MBSegmentsUsed;				// offset: 112
	private static float s_percentTemporary4GBSegmentsUsed;					// offset: 116
	private static float s_percentPermanent4GBSegmentUsed;					// offset: 124
	private static float s_percentCurrentInteractivePerformance;			// offset: 128
	private static float s_percentUncappedCPUCapacityUsed;					// offset: 132
	private static float s_percentSharedProcessorPoolUsed;					// offset: 136

	// SSTS0300 Format fields
	private static int s_poolsNumber;										// offset:  32

	
	public static void main(String[] args) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
		String strAs400 = System.getenv("AS400HOST");
		String strUser = System.getenv("USERID");
		String strPass = System.getenv("PASSWD");
		AS400 as400 = new AS400(strAs400, strUser, strPass);


        try
        {
            // Run the program then sleep. We run the program twice because
            // the first set of results are inflated. If we discard the first
            // set of results and run the command again five seconds later the
            // number will be more accurate.
        	byte[] as400Data = retrieveSystemStatus(as400, "SSTS0200", 256);
            
            Thread.sleep(5000);
            
            // Run the program
            as400Data = retrieveSystemStatus(as400, "SSTS0200", 256);
            if (s_getSystemStatus_Result != true)
            {
                // If the program did not run get the list of error messages
                // from the program object and display the messages. The error
                // would be something like program-not-found or not-authorized
                // to the program.
            	if (GetSystemStatus.s_getSystemStatus != null) 
            	{
                    AS400Message[] msgList = GetSystemStatus.s_getSystemStatus.getMessageList();
                    System.err.println("The program did not run. Server messages:");
                    for (int i=0; i<msgList.length; i++)
                    {
                        System.err.println(msgList[i].getText());
                    }
            	}
            	else 
            	{
            		System.err.println("The program did not run. Unknown reason.");
            	}
            	System.exit(-1);
            }
            // Else the program did run.
            else
            {
            	getFormat0200_Data(as400, as400Data);
            }
            
            // Run the program
            as400Data = retrieveSystemStatus(as400, "SSTS0100", 256);
            if (s_getSystemStatus_Result != true)
            {
                // If the program did not run get the list of error messages
                // from the program object and display the messages. The error
                // would be something like program-not-found or not-authorized
                // to the program.
            	if (GetSystemStatus.s_getSystemStatus != null) 
            	{
                    AS400Message[] msgList = GetSystemStatus.s_getSystemStatus.getMessageList();
                    System.err.println("The program did not run. Server messages:");
                    for (int i=0; i<msgList.length; i++)
                    {
                        System.err.println(msgList[i].getText());
                    }
            	}
            	else 
            	{
            		System.err.println("The program did not run. Unknown reason.");
            	}
            	System.exit(-1);
            }
            // Else the program did run.
            else
            {
            	getFormat0100_Data(as400, as400Data);
            }
            
            // Run the program
            as400Data = retrieveSystemStatus(as400, "SSTS0300", 256);
            if (s_getSystemStatus_Result != true)
            {
                // If the program did not run get the list of error messages
                // from the program object and display the messages. The error
                // would be something like program-not-found or not-authorized
                // to the program.
            	if (GetSystemStatus.s_getSystemStatus != null) 
            	{
                    AS400Message[] msgList = GetSystemStatus.s_getSystemStatus.getMessageList();
                    System.err.println("The program did not run. Server messages:");
                    for (int i=0; i<msgList.length; i++)
                    {
                        System.err.println(msgList[i].getText());
                    }
            	}
            	else 
            	{
            		System.err.println("The program did not run. Unknown reason.");
            	}
            	System.exit(-1);
            }
            // Else the program did run.
            else
            {
            	getFormat0300_Data(as400, as400Data);
            }

            String strJson = getJsonText();
            System.out.println(strJson);
            
            // This program is done running program so disconnect from
            // the command server on the server. Program call and command
            // call use the same server on the server.
            as400.disconnectService(AS400.COMMAND);
        }
        catch (Exception e)
        {
            // If any of the above operations failed say the program failed
            // and output the exception.
            System.err.println("Program call failed");
            System.err.println(e);
        }
	}
	
	private static byte[] retrieveSystemStatus(AS400 as400, String format, int bufferLth) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
	
        try
        {
        	s_getSystemStatus_Result = false;
        	
	    	// Create the path to the program.
	        QSYSObjectPathName programName = new QSYSObjectPathName("QSYS", "QWCRSSTS", "PGM");
	        
	        // Create the program call object. Associate the object with the
	        // AS400 object that represents the server we get status from.
	        //ProgramCall getSystemStatus = new ProgramCall(as400);
	        GetSystemStatus.s_getSystemStatus = new ProgramCall(as400);
	        
	        // Create the program parameter list. This program has five
	        // parameters that will be added to this list.
	        ProgramParameter[] parmlist = new ProgramParameter[5];
	        
	        // The server program returns data in parameter 1. It is an output
	        // parameter. Allocate bufferLth bytes for this parameter.
	        parmlist[0] = new ProgramParameter( bufferLth );
	        
	        // Parameter 2 is the buffer size of parm 1. It is a numeric input
	        // parameter. Sets its value to bufferLth, convert it to the server format,
	        // then add the parm to the parm list.
	        AS400Bin4 bin4 = new AS400Bin4( );
	        int iStatusLength = bufferLth;
	        byte[] statusLength = bin4.toBytes( iStatusLength );
	        parmlist[1] = new ProgramParameter( statusLength );
	        
	        // Parameter 3 is the status-format parameter. It is a string input
	        // parameter. Set the string value, convert it to the server format,
	        // then add the parameter to the parm list.
	        AS400Text text1 = new AS400Text(8, as400);
	        byte[] statusFormat = text1.toBytes(format);
	        parmlist[2] = new ProgramParameter( statusFormat );
	        
	        // Parameter 4 is the reset-statistics parameter. It is a string input
	        // parameter. Set the string value, convert it to the server format,
	        // then add the parameter to the parm list.
	        AS400Text text3 = new AS400Text(10, as400);
	        byte[] resetStats = text3.toBytes("*NO ");
	        parmlist[3] = new ProgramParameter( resetStats );
	        
	        // Parameter 5 is the error info parameter. It is an input/output
	        // parameter. Add it to the parm list.
	        byte[] errorInfo = new byte[32];
	        parmlist[4] = new ProgramParameter( errorInfo, 0 );
	
	        // Set the program to call and the parameter list to the program
	        // call object.
	        GetSystemStatus.s_getSystemStatus.setProgram(programName.getPath(), parmlist );
	        
            // Run the program
	        s_getSystemStatus_Result = GetSystemStatus.s_getSystemStatus.run();
            if (s_getSystemStatus_Result != true)
            {
                // If the program did not run get the list of error messages
                // from the program object and display the messages. The error
                // would be something like program-not-found or not-authorized
                // to the program.
                AS400Message[] msgList = GetSystemStatus.s_getSystemStatus.getMessageList();
                System.err.println("The program did not run. Server messages:");
                for (int i=0; i<msgList.length; i++)
                {
                    System.err.println(msgList[i].getText());
                }
                return null;
            }
            // Else the program did run.
            else
            {
                /////////////////////////////////////////////////////
                // Get the results of the program. Output data is in
                // a byte array in the first parameter.
                byte[] as400Data = parmlist[0].getOutputData();

                return as400Data;
            }
        }
        catch (Exception e)
        {
            // If any of the above operations failed say the program failed
            // and output the exception.
            System.err.println("Program call failed");
            System.err.println(e);
            return null;
        }
		
	}
	
	private static void getFormat0100_Data(AS400 as400, byte[] as400Data) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException  {

    	Integer anInteger;
        AS400Bin4 as400Int = new AS400Bin4();


		// Date s_dateTimeStatusGathered;							// offset:   8
        byte[] timeStamp = new byte[8];
        for (int x=0; x < 8; x++)
        {
        	timeStamp[x] = as400Data[8+x];
        }
        DateTimeConverter dtConverter = new DateTimeConverter(as400);
        s_dateTimeStatusGathered = dtConverter.convert(timeStamp, "*DTS"); 
        
        
        // int s_currentUsersSignedOn;								// offset: 	24
        anInteger  = (Integer)as400Int.toObject(as400Data, 24);
        s_currentUsersSignedOn =  anInteger.intValue();

		// int s_usersSuspendedBySystemRequest;						// offset:  32
        anInteger  = (Integer)as400Int.toObject(as400Data, 32);
        s_usersSuspendedBySystemRequest =  anInteger.intValue();

		// int s_usersSignedOffWithPrinterOutputWaitingToPrint;		// offset:  40
        anInteger  = (Integer)as400Int.toObject(as400Data, 40);
        s_usersSignedOffWithPrinterOutputWaitingToPrint =  anInteger.intValue();

		// int s_numberOfBatchJobsWaitingForMessage;				// offset:  44
        anInteger  = (Integer)as400Int.toObject(as400Data, 44);
        s_numberOfBatchJobsWaitingForMessage =  anInteger.intValue();

		// int s_numberOfBatchJobsRunning;							// offset:  48
        anInteger  = (Integer)as400Int.toObject(as400Data, 48);
        s_numberOfBatchJobsRunning =  anInteger.intValue();

		// int s_numberOfBatchJobsEnding;							// offset:  56
        anInteger  = (Integer)as400Int.toObject(as400Data, 56);
        s_numberOfBatchJobsEnding =  anInteger.intValue();

		// int s_numberOfBatchJobsWaitingToRunOrAlreadyScheduled;	// offset:  60
        anInteger  = (Integer)as400Int.toObject(as400Data, 60);
        s_numberOfBatchJobsWaitingToRunOrAlreadyScheduled =  anInteger.intValue();

		// int s_numberOfBatchJobsHeldOnQueue;						// offset:  64
        anInteger  = (Integer)as400Int.toObject(as400Data, 64);
        s_numberOfBatchJobsHeldOnQueue =  anInteger.intValue();

		// int s_numberOfBatchJobsOnUnassignedQueues;				// offset:  72
        anInteger  = (Integer)as400Int.toObject(as400Data, 72);
        s_numberOfBatchJobsOnUnassignedQueues =  anInteger.intValue();

		// int s_batchJobsEndedWithPrinterOutputWaitingToPrint;		// offset:  76
        anInteger  = (Integer)as400Int.toObject(as400Data, 76);
        s_batchJobsEndedWithPrinterOutputWaitingToPrint =  anInteger.intValue();

	}

	private static void getFormat0200_Data(AS400 as400, byte[] as400Data)  {
    	String aStr;
    	Integer anInteger;
    	AS400Text as400Text;
        AS400Bin4 as400Int = new AS400Bin4();

		// String systemName;									// offset:  16
        as400Text = new AS400Text(8, as400 );
        s_systemName  = (String)as400Text.toObject(as400Data, 16);
        
		// boolean restrictedStateFlag;							// offset:  30
        as400Text = new AS400Text(1, as400);
        aStr  = (String)as400Text.toObject(as400Data, 30);
        
        if (aStr.equals("0"))
        	s_restrictedStateFlag = false;
        else
        	s_restrictedStateFlag  = true;
        
		// float percentProcessingUnitUsed;						// offset:  32
        anInteger  = (Integer)as400Int.toObject(as400Data, 32);
        s_percentProcessingUnitUsed = (float)anInteger / 10;
        
    	// int s_numberOfJobsInSystem;							// offset:  36
        anInteger  = (Integer)as400Int.toObject(as400Data, 36);
        s_numberOfJobsInSystem =  anInteger.intValue();

		// float percentTemporaryAddresses;						// offset:  44
        anInteger  = (Integer)as400Int.toObject(as400Data, 44);
        s_percentTemporaryAddresses = (float)anInteger / 1000;
        
		// int systemASP;										// offset:  48
        anInteger  = (Integer)as400Int.toObject(as400Data, 48);
        s_systemASP =  anInteger.intValue();
        
		// float percentSystemASPUsed;							// offset:  52
        anInteger  = (Integer)as400Int.toObject(as400Data, 52);
        s_percentSystemASPUsed = (float)anInteger / 10000;

        // int totalAuxiliaryStorage;							// offset:  56
        anInteger  = (Integer)as400Int.toObject(as400Data, 56);
        s_totalAuxiliaryStorage =  anInteger.intValue();
        
		// int currentUnprotectedStorageUsed;					// offset:  60
        anInteger  = (Integer)as400Int.toObject(as400Data, 60);
        s_currentUnprotectedStorageUsed =  anInteger.intValue();
        
		// int maxUnprotectedStorageUsed;						// offset:  64
        anInteger  = (Integer)as400Int.toObject(as400Data, 64);
        s_maxUnprotectedStorageUsed =  anInteger.intValue();
        
		// float percentDBCapability;							// offset:  68
        anInteger  = (Integer)as400Int.toObject(as400Data, 68);
        if (anInteger.intValue() == -1)
        	s_percentDBCapability = -1;
        else
        	s_percentDBCapability = (float)anInteger / 10;

		// long mainStorageSize;								// offset:  72
        anInteger  = (Integer)as400Int.toObject(as400Data, 72);
        s_mainStorageSize = anInteger.longValue();

		// int numberOfPartitions;								// offset:  76
        anInteger  = (Integer)as400Int.toObject(as400Data, 76);
        s_numberOfPartitions =  anInteger.intValue();
        
		// int partitionIdentifier;								// offset:  80
        anInteger  = (Integer)as400Int.toObject(as400Data, 80);
        s_partitionIdentifier =  anInteger.intValue();
        
		// float currentProcessingCapacity;						// offset:  88
        anInteger  = (Integer)as400Int.toObject(as400Data, 88);
        s_currentProcessingCapacity = (float)anInteger / 100;

		// int processorSharingAttribute;						// offset:  92
        as400Text = new AS400Text(1, as400);
        aStr  = (String)as400Text.toObject(as400Data, 92);
        s_processorSharingAttribute = Integer.valueOf(aStr);
        
		// int numberOfProcessors;								// offset:  96
        anInteger  = (Integer)as400Int.toObject(as400Data, 96);
        s_numberOfProcessors =  anInteger.intValue();
        
		// int numberActiveJobsInSystem;						// offset: 100
        anInteger  = (Integer)as400Int.toObject(as400Data, 100);
        s_numberActiveJobsInSystem =  anInteger.intValue();
        
		// int activeThreadsInSystem;							// offset: 104
        anInteger  = (Integer)as400Int.toObject(as400Data, 104);
        s_activeThreadsInSystem =  anInteger.intValue();
        
		// long maxJobsInSystem;								// offset: 108
        anInteger  = (Integer)as400Int.toObject(as400Data, 108);
        s_maxJobsInSystem = anInteger.longValue();

        // float percentTemporary256MBSegmentsUsed;				// offset: 112
        anInteger  = (Integer)as400Int.toObject(as400Data, 112);
        s_percentTemporary256MBSegmentsUsed = (float)anInteger / 1000;

		// float percentTemporary4GBSegmentsUsed;				// offset: 116
        anInteger  = (Integer)as400Int.toObject(as400Data, 116);
        s_percentTemporary4GBSegmentsUsed = (float)anInteger / 1000;

		// float percentPermanent4GBSegmentUsed;				// offset: 124
        anInteger  = (Integer)as400Int.toObject(as400Data, 124);
        s_percentPermanent4GBSegmentUsed = (float)anInteger / 1000;

		// float percentCurrentInteractivePerformance;			// offset: 128
        anInteger  = (Integer)as400Int.toObject(as400Data, 128);
        s_percentCurrentInteractivePerformance = (float)anInteger / 1;

		// float percentUncappedCPUCapacityUsed;				// offset: 132
        anInteger  = (Integer)as400Int.toObject(as400Data, 132);
        if (anInteger.intValue() == -1)
        	s_percentUncappedCPUCapacityUsed = -1;
        else
        	s_percentUncappedCPUCapacityUsed = (float)anInteger / 10;

		// float percentSharedProcessorPoolUsed					// offset: 136
        anInteger  = (Integer)as400Int.toObject(as400Data, 136);
        if (anInteger.intValue() == -1)
        	s_percentSharedProcessorPoolUsed = -1;
        else
        	s_percentSharedProcessorPoolUsed = (float)anInteger / 10;

	}

	private static void getFormat0300_Data(AS400 as400, byte[] as400Data)  {
    	Integer anInteger;
        AS400Bin4 as400Int = new AS400Bin4();

		// int s_poolsNumber;										// offset:  32
        anInteger  = (Integer)as400Int.toObject(as400Data, 32);
        s_poolsNumber =  anInteger.intValue();

	}
	
	private static String getJsonText()  {
		String strNrName = "com.newrelic.as400-system-status";
		String strNrEventType = "AS400:SystemStatusEvent";
		String strNrProtoVersion = "1";
		String strNrIntVersion = "0.2.0";
		String strJSONMetrics = "";
		String strJSONHeader = ("{" + "\"name\":" + '"' + strNrName + '"' + "," + "\"protocol_version\":" + '"' + strNrProtoVersion + '"' + "," + "\"integration_version\":" + '"' + strNrIntVersion + '"' + "," + "\"metrics\":" + "[");
		String strJSONFooter = ("]," + "\"inventory\":" + "{" + "}," + "\"events\":" + "[" + "]" + "}");

		strJSONMetrics = strJSONMetrics +
				"{" +
					"\"event_type\":" +
					'"' +
					strNrEventType +
					'"' +
					"," +
					"\"numberActiveJobsInSystem\":" +
					s_numberActiveJobsInSystem +
					"," +
					"\"activeThreadsInSystem\":" +
					s_activeThreadsInSystem +
					"," +
					"\"batchJobsEndedWithPrinterOutputWaitingToPrint\":" +
					s_batchJobsEndedWithPrinterOutputWaitingToPrint +
					"," +
					"\"numberOfBatchJobsEnding\":" +
					s_numberOfBatchJobsEnding +
					"," +
					"\"numberOfBatchJobsHeldOnQueue\":" +
					s_numberOfBatchJobsHeldOnQueue +
					"," +
					"\"numberOfBatchJobsOnUnassignedQueues\":" +
					s_numberOfBatchJobsOnUnassignedQueues +
					"," +
					"\"numberOfBatchJobsRunning\":" +
					s_numberOfBatchJobsRunning +
					"," +
					"\"numberOfBatchJobsWaitingForMessage\":" +
					s_numberOfBatchJobsWaitingForMessage +
					"," +
					"\"numberOfBatchJobsWaitingToRunOrAlreadyScheduled\":" +
					s_numberOfBatchJobsWaitingToRunOrAlreadyScheduled +
					"," +
					"\"currentProcessingCapacity\":" +
					s_currentProcessingCapacity +
					"," +
					"\"currentUnprotectedStorageUsed\":" +
					s_currentUnprotectedStorageUsed +
					"," +
					"\"dateTimeStatusGathered\":" +
					'"' +
					s_dateTimeStatusGathered +
					'"' +
					"," +
					"\"numberOfJobsInSystem\":" +
					s_numberOfJobsInSystem +
					"," +
					"\"mainStorageSize\":" +
					s_mainStorageSize +
					"," +
					"\"maxJobsInSystem\":" +
					s_maxJobsInSystem +
					"," +
					"\"maxUnprotectedStorageUsed\":" +
					s_maxUnprotectedStorageUsed +
					"," +
					"\"numberOfPartitions\":" +
					s_numberOfPartitions +
					"," +
					"\"numberOfProcessors\":" +
					s_numberOfProcessors +
					"," +
					"\"partitionIdentifier\":" +
					'"' +
					s_partitionIdentifier +
					'"' +
					"," +
					"\"percentCurrentInteractivePerformance\":" +
					s_percentCurrentInteractivePerformance +
					"," +
					"\"percentDBCapability\":" +
					s_percentDBCapability +
					"," +
					"\"percentPermanent4GBSegmentUsed\":" +
					s_percentPermanent4GBSegmentUsed +
					"," +
					"\"percentProcessingUnitUsed\":" +
					s_percentProcessingUnitUsed +
					"," +
					"\"percentSharedProcessorPoolUsed\":" +
					s_percentSharedProcessorPoolUsed +
					"," +
					"\"percentSystemASPUsed\":" +
					s_percentSystemASPUsed +
					"," +
					"\"percentTemporary256MBSegmentsUsed\":" +
					s_percentTemporary256MBSegmentsUsed +
					"," +
					"\"percentTemporary4GBSegmentsUsed\":" +
					s_percentTemporary4GBSegmentsUsed +
					"," +
					"\"percentTemporaryAddresses\":" +
					s_percentTemporaryAddresses +
					"," +
					"\"percentUncappedCPUCapacityUsed\":" +
					s_percentUncappedCPUCapacityUsed +
					"," +
					"\"poolsNumber\":" +
					s_poolsNumber +
					"," +
					"\"processorSharingAttribute\":" +
					s_processorSharingAttribute +
					"," +
					"\"restrictedStateFlag\":" +
					'"' +
					s_restrictedStateFlag +
					'"' +
					"," +
					"\"systemASP\":" +
					s_systemASP +
					"," +
					"\"systemName\":" +
					'"' +
					s_systemName +
					'"' +
					"," +
					"\"totalAuxiliaryStorage\":" +
					s_totalAuxiliaryStorage +
					"," +
					"\"currentUsersSignedOn\":" +
					s_currentUsersSignedOn +
					"," +
					"\"usersSignedOffWithPrinterOutputWaitingToPrint\":" +
					s_usersSignedOffWithPrinterOutputWaitingToPrint +
					"," +
					"\"usersSuspendedBySystemRequest\":" +
					s_usersSuspendedBySystemRequest +
					"}";
		return strJSONHeader + strJSONMetrics + strJSONFooter;

	}
}
