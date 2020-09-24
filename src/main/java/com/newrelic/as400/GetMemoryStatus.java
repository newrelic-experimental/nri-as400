//  ----------------------------------------------------------------------
//
//  Name: GetMemoryStatus
//
//  ----------------------------------------------------------------------
//  Modification Log
//  ----------------------------------------------------------------------
//  9/2/2020 - Initial creation - EK
//  ----------------------------------------------------------------------
///////////////////////////////////////////////////////////////////////////
package com.newrelic.as400;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import com.ibm.as400.access.*;

public class GetMemoryStatus {
	
	private static  ProgramCall s_getSystemStatus = null;
	private static boolean s_getSystemStatus_Result = false;

	// SSTS0400 Common Header Area
	private static int s_ssts0400_bytesAvailable;                   // offset: 0
	private static int s_ssts0400_bytesReturned;                    // offset: 4
	private static Date s_ssts0400_currentDateAndTime;              // offset: 8
	private static String s_ssts0400_systemName;                    // offset: 16
	private static String s_ssts0400_elapsedTime;                   // offset: 24
	private static int s_ssts0400_mainStorageSize;                  // offset: 32
	private static int s_ssts0400_minimumMachinePoolSize;           // offset: 36
	private static int s_ssts0400_minimumBasePoolSize;              // offset: 40
	private static int s_ssts0400_numberOfPools;                    // offset: 44
	private static int s_ssts0400_offsetToPoolInformation;          // offset: 48
	private static int s_ssts0400_lengthOfPoolInformationEntry;     // offset: 52
	private static Double s_ssts0400_mainStorageSize_long;          // offset: 56
	private static Double s_ssts0400_minimumMachinePoolSize_long;   // offset: 64
	private static Double s_ssts0400_minimumBasePoolSize_long;      // offset: 72

	private static SSTS0400_PoolEntry[] s_ssts0400_poolEntries;
	
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
        	byte[] as400Data;// = retrieveSystemStatus(as400, "SSTS0400", 2400, true);
            
            Thread.sleep(5000);

            // Run the program
            as400Data = retrieveSystemStatus(as400, "SSTS0400", 2400, false);
            if (s_getSystemStatus_Result != true)
            {
                // If the program did not run get the list of error messages
                // from the program object and display the messages. The error
                // would be something like program-not-found or not-authorized
                // to the program.
            	if (s_getSystemStatus != null) 
            	{
                    AS400Message[] msgList = s_getSystemStatus.getMessageList();
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
            	getFormat0400_Data(as400, as400Data);
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
	
	private static byte[] retrieveSystemStatus(AS400 as400, String format, int bufferLth, boolean resetStats) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
	
        try
        {
        	s_getSystemStatus_Result = false;
        	
	    	// Create the path to the program.
	        QSYSObjectPathName programName = new QSYSObjectPathName("QSYS", "QWCRSSTS", "PGM");
	        
	        // Create the program call object. Associate the object with the
	        // AS400 object that represents the server we get status from.
	        //ProgramCall getSystemStatus = new ProgramCall(as400);
	        s_getSystemStatus = new ProgramCall(as400);
	        
	        // Create the program parameter list. This program has five
	        // parameters that will be added to this list.
	        int numOfParms = 5;
	        if (format.equals("SSTS0400")) {
	        	numOfParms = 7;
	        }
	        ProgramParameter[] parmlist = new ProgramParameter[numOfParms];
	        
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
	        byte[] resetStatsParm;
	        if (resetStats) {
	        	resetStatsParm = text3.toBytes("*YES");
	        } else {
	        	resetStatsParm = text3.toBytes("*NO ");
	        }
	        parmlist[3] = new ProgramParameter( resetStatsParm );
	        
	        // Parameter 5 is the error info parameter. It is an input/output
	        // parameter. Add it to the parm list.
	        byte[] errorInfo = new byte[32];
	        parmlist[4] = new ProgramParameter( errorInfo, 0 );
	
	        if (format.equals("SSTS0400")) {
	        	byte[] poolSelectionInfo = new byte[24];
	        	
	        	AS400Text text4 = new AS400Text(10, as400);
	        	byte[] typeOfPool = text4.toBytes("*SYSTEM");
	        	AS400Text text5 = new AS400Text(10, as400);
	        	byte[] sharedPoolName = text5.toBytes("");
	        	bin4 = new AS400Bin4();
	        	int returnAllPools = -1;
	        	byte[] systemPoolIdentifier = bin4.toBytes(returnAllPools);
	        	System.arraycopy(typeOfPool, 0, poolSelectionInfo, 0, 10);
	        	System.arraycopy(sharedPoolName, 0, poolSelectionInfo, 10, 10);
	        	System.arraycopy(systemPoolIdentifier, 0, poolSelectionInfo, 20, 4);
	        	
	        	parmlist[5] = new ProgramParameter( poolSelectionInfo );
	        	
	        	bin4 = new AS400Bin4();
	        	byte[] sizeOfPoolSelectionInfo = bin4.toBytes(24);
	        	parmlist[6] = new ProgramParameter( sizeOfPoolSelectionInfo );
	        	
	        }

	        // Set the program to call and the parameter list to the program
	        // call object.
	        s_getSystemStatus.setProgram(programName.getPath(), parmlist );
	        
            // Run the program
	        s_getSystemStatus_Result = s_getSystemStatus.run();
            if (s_getSystemStatus_Result != true)
            {
                // If the program did not run get the list of error messages
                // from the program object and display the messages. The error
                // would be something like program-not-found or not-authorized
                // to the program.
                AS400Message[] msgList = s_getSystemStatus.getMessageList();
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


	private static void getFormat0400_Data(AS400 as400, byte[] as400Data) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException  {
    	Integer anInteger;
        AS400Bin4 as400Int = new AS400Bin4();
    	AS400Text as400Text;
    	byte[] ba8;

    	// int s_ssts0400_bytesAvailable;                   // offset: 0
        anInteger  = (Integer)as400Int.toObject(as400Data, 0);
        s_ssts0400_bytesAvailable =  anInteger.intValue();

        // int s_ssts0400_bytesReturned;                    // offset: 4
        anInteger  = (Integer)as400Int.toObject(as400Data, 4);
        s_ssts0400_bytesReturned =  anInteger.intValue();

		// Date s_ssts0400_currentDateAndTime;              // offset: 8
        byte[] timeStamp = new byte[8];
        for (int x=0; x < 8; x++)
        {
        	timeStamp[x] = as400Data[8+x];
        }
        DateTimeConverter dtConverter = new DateTimeConverter(as400);
        s_ssts0400_currentDateAndTime = dtConverter.convert(timeStamp, "*DTS"); 

        // String s_ssts0400_systemName;                    // offset: 16
        as400Text = new AS400Text(8, as400 );
        s_ssts0400_systemName  = ((String)as400Text.toObject(as400Data, 16)).trim();

        // String s_ssts0400_elapsedTime;                   // offset: 24
        as400Text = new AS400Text(6, as400 );
        s_ssts0400_elapsedTime  = (String)as400Text.toObject(as400Data, 24);

        // int s_ssts0400_mainStorageSize;                  // offset: 32
        anInteger  = (Integer)as400Int.toObject(as400Data, 32);
        s_ssts0400_mainStorageSize =  anInteger.intValue();

		// int s_ssts0400_minimumMachinePoolSize;           // offset: 36
        anInteger  = (Integer)as400Int.toObject(as400Data, 36);
        s_ssts0400_minimumMachinePoolSize =  anInteger.intValue();

		// int s_ssts0400_minimumBasePoolSize;              // offset: 40
        anInteger  = (Integer)as400Int.toObject(as400Data, 40);
        s_ssts0400_minimumBasePoolSize =  anInteger.intValue();

		// int s_ssts0400_numberOfPools;                    // offset: 44
        anInteger  = (Integer)as400Int.toObject(as400Data, 44);
        s_ssts0400_numberOfPools =  anInteger.intValue();

		// int s_ssts0400_offsetToPoolInformation;          // offset: 48
        anInteger  = (Integer)as400Int.toObject(as400Data, 48);
        s_ssts0400_offsetToPoolInformation =  anInteger.intValue();

		// int s_ssts0400_lengthOfPoolInformationEntry;     // offset: 52
        anInteger  = (Integer)as400Int.toObject(as400Data, 52);
        s_ssts0400_lengthOfPoolInformationEntry =  anInteger.intValue();

		// Double s_ssts0400_mainStorageSize_long;          // offset: 56
        ba8 = new byte[8];
        for (int y=0; y < 8; y++)
        {
        	ba8[y] = as400Data[56+y];
        }
        s_ssts0400_mainStorageSize_long = toDouble(ba8);

		// Double s_ssts0400_minimumMachinePoolSize_long;   // offset: 64
        ba8 = new byte[8];
        for (int y=0; y < 8; y++)
        {
        	ba8[y] = as400Data[64+y];
        }
        s_ssts0400_minimumMachinePoolSize_long = toDouble(ba8);
		
        // Double s_ssts0400_minimumBasePoolSize_long;      // offset: 72
        ba8 = new byte[8];
        for (int y=0; y < 8; y++)
        {
        	ba8[y] = as400Data[72+y];
        }
        s_ssts0400_minimumBasePoolSize_long = toDouble(ba8);     
        
        int numOfPoolEntries = (s_ssts0400_bytesReturned - s_ssts0400_offsetToPoolInformation) 
        		/ s_ssts0400_lengthOfPoolInformationEntry;
        
        //System.err.println("Number Of SSTS0400 Pool Entries:" + numOfPoolEntries);

        if (numOfPoolEntries > 0) {
        	s_ssts0400_poolEntries = new SSTS0400_PoolEntry[numOfPoolEntries];
        	
        	int x = s_ssts0400_offsetToPoolInformation;
        	for (int entry = 0; entry < numOfPoolEntries; entry++)
        	{
            	// SSTS0400 Pool Information (repeats)

        		SSTS0400_PoolEntry poolEntry = new SSTS0400_PoolEntry();

        		// int m_ssts0400_systemPool;                       // offset: 0
                anInteger  = (Integer)as400Int.toObject(as400Data, x+0);
                poolEntry.m_ssts0400_systemPool =  anInteger.intValue();
                
                // int m_ssts0400_poolSize;                         // offset: 4
                anInteger  = (Integer)as400Int.toObject(as400Data, x+4);
                poolEntry.m_ssts0400_poolSize =  anInteger.intValue();
                
        		// int m_ssts0400_reservedSize;                     // offset: 8
                anInteger  = (Integer)as400Int.toObject(as400Data, x+8);
                poolEntry.m_ssts0400_reservedSize =  anInteger.intValue();
                
        		// int m_ssts0400_maximumActiveThreads;             // offset: 12
                anInteger  = (Integer)as400Int.toObject(as400Data, x+12);
                poolEntry.m_ssts0400_maximumActiveThreads =  anInteger.intValue();
                
        		// float m_ssts0400_databaseFaults;                   // offset: 16
                anInteger  = (Integer)as400Int.toObject(as400Data, x+16);
                poolEntry.m_ssts0400_databaseFaults =  (float)anInteger / 10;
                
        		// float m_ssts0400_databasePages;                    // offset: 20
                anInteger  = (Integer)as400Int.toObject(as400Data, x+20);
                poolEntry.m_ssts0400_databasePages =  (float)anInteger / 10;
                
        		// float m_ssts0400_nondatabaseFaults;                // offset: 24
                anInteger  = (Integer)as400Int.toObject(as400Data, x+24);
                poolEntry.m_ssts0400_nondatabaseFaults =  (float)anInteger / 10;
                
        		// float m_ssts0400_nondatabasePages;                 // offset: 28
                anInteger  = (Integer)as400Int.toObject(as400Data, x+28);
                poolEntry.m_ssts0400_nondatabasePages =  (float)anInteger / 10;
                
        		// float m_ssts0400_activeToWait;                     // offset: 32
                anInteger  = (Integer)as400Int.toObject(as400Data, x+32);
                poolEntry.m_ssts0400_activeToWait =  (float)anInteger / 10;
                
        		// float m_ssts0400_waitToIneligible;                 // offset: 36
                anInteger  = (Integer)as400Int.toObject(as400Data, x+36);
                poolEntry.m_ssts0400_waitToIneligible =  (float)anInteger / 10;
                
        		// int m_ssts0400_activeToIneligible;               // offset: 40
                anInteger  = (Integer)as400Int.toObject(as400Data, x+40);
                poolEntry.m_ssts0400_activeToIneligible =  anInteger.intValue();
                
        		// String m_ssts0400_poolName;                      // offset: 44
                as400Text = new AS400Text(10, as400 );
                poolEntry.m_ssts0400_poolName  = ((String)as400Text.toObject(as400Data, x+44)).trim();

                // String m_ssts0400_subsystemName;                 // offset: 54
                as400Text = new AS400Text(10, as400 );
                poolEntry.m_ssts0400_subsystemName  = ((String)as400Text.toObject(as400Data, x+54)).trim();

        		// String m_ssts0400_subsystemLibraryName;          // offset: 64
                as400Text = new AS400Text(10, as400 );
                poolEntry.m_ssts0400_subsystemLibraryName  = ((String)as400Text.toObject(as400Data, x+64)).trim();

        		// String m_ssts0400_pagingOption;                  // offset: 74
                as400Text = new AS400Text(10, as400 );
                poolEntry.m_ssts0400_pagingOption  = ((String)as400Text.toObject(as400Data, x+74)).trim();
        		
                // int m_ssts0400_definedSize;                      // offset: 84
                anInteger  = (Integer)as400Int.toObject(as400Data, x+84);
                poolEntry.m_ssts0400_definedSize =  anInteger.intValue();
                
        		// int m_ssts0400_currentThreads;                   // offset: 88
                anInteger  = (Integer)as400Int.toObject(as400Data, x+88);
                poolEntry.m_ssts0400_currentThreads =  anInteger.intValue();
                
        		// int m_ssts0400_currentIneligibleThreads;         // offset: 92
                anInteger  = (Integer)as400Int.toObject(as400Data, x+92);
                poolEntry.m_ssts0400_currentIneligibleThreads =  anInteger.intValue();
                
        		// int m_ssts0400_tuningPriority;                   // offset: 96
                anInteger  = (Integer)as400Int.toObject(as400Data, x+96);
                poolEntry.m_ssts0400_tuningPriority =  anInteger.intValue();
                
        		// int m_ssts0400_tuningMinimumPoolSizePct;         // offset: 100
                anInteger  = (Integer)as400Int.toObject(as400Data, x+100);
                poolEntry.m_ssts0400_tuningMinimumPoolSizePct =  anInteger.intValue();
                
        		// int m_ssts0400_tuningMaximumPoolSizePct;         // offset: 104
                anInteger  = (Integer)as400Int.toObject(as400Data, x+104);
                poolEntry.m_ssts0400_tuningMaximumPoolSizePct =  anInteger.intValue();
                
        		// int m_ssts0400_tuningMinimumFaults;              // offset: 108
                anInteger  = (Integer)as400Int.toObject(as400Data, x+108);
                poolEntry.m_ssts0400_tuningMinimumFaults =  anInteger.intValue();
                
        		// float m_ssts0400_tuningPerThreadFaults;            // offset: 112
                anInteger  = (Integer)as400Int.toObject(as400Data, x+112);
                poolEntry.m_ssts0400_tuningPerThreadFaults =  (float)anInteger / 100;
                
        		// float m_ssts0400_tuningNaximumFaults;              // offset: 116
                anInteger  = (Integer)as400Int.toObject(as400Data, x+116);
                poolEntry.m_ssts0400_tuningMaximumFaults =  (float)anInteger / 100;
                
        		// String m_ssts0400_description;                   // offset: 120
                as400Text = new AS400Text(50, as400 );
                poolEntry.m_ssts0400_description  = ((String)as400Text.toObject(as400Data, x+120)).trim();

        		// String m_ssts0400_status;                        // offset: 170
                as400Text = new AS400Text(1, as400 );
                poolEntry.m_ssts0400_status  = (String)as400Text.toObject(as400Data, x+170);


                // int m_ssts0400_tuningMinimumActivityLevel;       // offset: 172
                anInteger  = (Integer)as400Int.toObject(as400Data, x+172);
                poolEntry.m_ssts0400_tuningMinimumActivityLevel =  anInteger.intValue();
                
        		// int m_ssts0400_tuningMaximumActivityLevel;       // offset: 176
                anInteger  = (Integer)as400Int.toObject(as400Data, x+176);
                poolEntry.m_ssts0400_tuningMaximumActivityLevel =  anInteger.intValue();
                
        		// Double m_ssts0400_poolSize_long;                 // offset: 180
                ba8 = new byte[8];
                for (int y=0; y < 8; y++)
                {
                	ba8[y] = as400Data[x+180+y];
                }
                poolEntry.m_ssts0400_poolSize_long = toDouble(ba8);

                // Double m_ssts0400_definedSize_long;              // offset: 188
                ba8 = new byte[8];
                for (int y=0; y < 8; y++)
                {
                	ba8[y] = as400Data[x+188+y];
                }
                poolEntry.m_ssts0400_definedSize_long = toDouble(ba8);

                
                s_ssts0400_poolEntries[entry] = poolEntry;
                
                x += s_ssts0400_lengthOfPoolInformationEntry;
        		
        	}
            //System.err.println("NumberOfPoolEntries:" + numOfPoolEntries);
        }
	}

	private static double toDouble(byte[] bytes) {
	    return ByteBuffer.wrap(bytes).getDouble();
	}
	
	private static String getJsonText()  {
		String strNrName = "com.newrelic.as400-memory-status";
		String strNrEventType = "AS400:MemoryStatusEvent";
		String strNrProtoVersion = "1";
		String strNrIntVersion = "0.2.0";
		String strJSONMetrics = "";
		String strJSONHeader = ("{" + "\"name\":" + '"' + strNrName + '"' + "," + "\"protocol_version\":" + '"' + strNrProtoVersion + '"' + "," + "\"integration_version\":" + '"' + strNrIntVersion + '"' + "," + "\"metrics\":" + "[");
		String strJSONFooter = ("]," + "\"inventory\":" + "{" + "}," + "\"events\":" + "[" + "]" + "}");
		
		String instanceGUID = java.util.UUID.randomUUID().toString();
		
		for (int x = 0; x < s_ssts0400_poolEntries.length; x++) {
			if (x > 0 ) {
				strJSONMetrics = strJSONMetrics +  ",";
			}
			
			SSTS0400_PoolEntry poolEntry = s_ssts0400_poolEntries[x];
			
			strJSONMetrics = strJSONMetrics +
					"{" +
						"\"event_type\":" +
						'"' +
						strNrEventType +
						'"' +
						"," +
						"\"eventInstanceId\":" +
						'"' +
						instanceGUID +
						'"' +
						"," +
						"\"systemName\":" +
						'"' +
						s_ssts0400_systemName +
						'"' +
						"," +
						"\"dateTimeStatusGathered\":" +
						'"' +
						s_ssts0400_currentDateAndTime +
						'"' +
						"," +
						"\"mainStorageSize\":" +
						s_ssts0400_mainStorageSize +
						"," +
						"\"minimumMachinePoolSize\":" +
						s_ssts0400_minimumMachinePoolSize +
						"," +
						"\"minimumBasePoolSize\":" +
						s_ssts0400_minimumBasePoolSize +
						"," +
						"\"numberOfPools\":" +
						s_ssts0400_numberOfPools +
						"," +
						"\"poolName\":" +
						'"' +
						poolEntry.m_ssts0400_poolName +
						'"' +
						"," +
						"\"subsystemName\":" +
						'"' +
						poolEntry.m_ssts0400_subsystemName +
						'"' +
						"," +
						"\"susbsystemLibraryName\":" +
						'"' +
						poolEntry.m_ssts0400_subsystemLibraryName +
						'"' +
						"," +
						"\"pagingOption\":" +
						'"' +
						poolEntry.m_ssts0400_pagingOption +
						'"' +
						"," +
						"\"description\":" +
						'"' +
						poolEntry.m_ssts0400_description +
						'"' +
						"," +
						"\"status\":" +
						'"' +
						poolEntry.m_ssts0400_status +
						'"' +
						"," +
						"\"systemPools\":" +
						poolEntry.m_ssts0400_systemPool +
						"," +
						"\"poolSize\":" +
						poolEntry.m_ssts0400_poolSize +
						"," +
						"\"maximumActiveThreads\":" +
						poolEntry.m_ssts0400_maximumActiveThreads +
						"," +
						"\"databaseFaults\":" +
						poolEntry.m_ssts0400_databaseFaults +
						"," +
						"\"databasePages\":" +
						poolEntry.m_ssts0400_databasePages +
						"," +
						"\"nondatabaseFaults\":" +
						poolEntry.m_ssts0400_nondatabaseFaults +
						"," +
						"\"nondatabasePages\":" +
						poolEntry.m_ssts0400_nondatabasePages +
						"," +
						"\"activeToWait\":" +
						poolEntry.m_ssts0400_activeToWait +
						"," +
						"\"waitToIneligible\":" +
						poolEntry.m_ssts0400_waitToIneligible +
						"," +
						"\"activeToIneligible\":" +
						poolEntry.m_ssts0400_activeToIneligible +
						"," +
						"\"definedSize\":" +
						poolEntry.m_ssts0400_definedSize +
						"," +
						"\"currentThreads\":" +
						poolEntry.m_ssts0400_currentThreads +
						"," +
						"\"currentIneligibleThreads\":" +
						poolEntry.m_ssts0400_currentIneligibleThreads +
						"}";
		}
		return strJSONHeader + strJSONMetrics + strJSONFooter;

	}
}