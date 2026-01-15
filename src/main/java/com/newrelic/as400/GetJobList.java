//  ----------------------------------------------------------------------
//
//  Name: GetJobList
//
//  ----------------------------------------------------------------------
//  Modification Log
//  ----------------------------------------------------------------------
//  2/9/2019 - Initial creation - MJ
//  9/2/2020 - Rewrote, make use of AS400 QUSRJOBI API  - EK
//  ----------------------------------------------------------------------
///////////////////////////////////////////////////////////////////////////
//
// This program is an example of the "job" classes in the
// IBM Toolbox for Java.  It gets a list of jobs on the server
// and outputs the job's status followed by job identifier.
//
//
// Command syntax:
//    ListJobs system userID password
//
// (UserID and password are optional)
//
/////////////////////////////////////////////////////////////////////////
package com.newrelic.as400;

import java.io.IOException;
import java.util.Enumeration;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Bin4;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.Job;
import com.ibm.as400.access.JobList;
import com.ibm.as400.access.JobLog;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;
import com.ibm.as400.access.SystemStatus;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;

public class GetJobList {
	private static String s_systemName;

	private static ProgramCall s_getJobInfo = null;
	private static boolean s_getJobInfo_Result = false;
	
	private static int s_resetWaitDelay;
	private static Boolean s_retrieve_msgw;

	// Json descriptor fields
	private static String s_strNrName = "com.newrelic.as400-job-list";
	private static String s_strNrIntVersion = "0.2.0";
	private static String s_strNrProtoVersion = "1";

	
	// JOBI0200 Format fields
	private static String s_jobName;			//o:8 l:8
	private static String s_jobUser;			//o:18 l:10
	private static String s_jobNumber;			//o:28 l:6
	private static float s_jobCPUUsed;			//o:80 l:4
	private static int s_jobRunPriority;		//o:72 l:4
	private static String s_jobStatus;			//o:50 l:10
	private static String s_activeJobStatus;	// o:107 l:4
	private static String s_jobSubsystem;		//o:62 l:10

	// JOBI0300 Format fields
	private static String s_jobQueue; 
	private static String s_jobQueuePriority;
	
	// JOBI1000 Format fields
	private static float s_jobCPUPct;
	
	// JobLog message
	private static String s_jobLogMessage;
	private static String s_jobLogMessageAdditionalInfo;
	private static String s_jobLogMessageHelp;


	public static void main(String[] args) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
		String strAs400 = System.getenv("AS400HOST");
		String strUser = System.getenv("USERID");
		String strPass = System.getenv("PASSWD");
		String strResetWait = System.getenv("RESET_WAIT_DELAY");
		s_resetWaitDelay = 100;		
		try {
			if (strResetWait != null)
			{
				if (!strResetWait.isEmpty()) {
					s_resetWaitDelay = Integer.parseInt(strResetWait);
				}
			}
		}
		catch (Exception e) {
			System.err.println("RESET_WAIT_DELAY not numeric: '" + strResetWait + "' - defaulting to 100ms");

			s_resetWaitDelay = 100;
		}

		String strGetJobLogMsg = System.getenv("RETRIEVE_MSGW");
		s_retrieve_msgw = false;
		try {
			if (strGetJobLogMsg != null)
			{
				if (!strGetJobLogMsg.isEmpty()) {
					if (strGetJobLogMsg.toLowerCase().startsWith("t") ||
						strGetJobLogMsg.toLowerCase().startsWith("y")) {
						s_retrieve_msgw = true;
					}
				}
			}
		}
		catch (Exception e) {
			System.err.println("RETRIEVE_MSGW invalid: '" + strGetJobLogMsg + "' - defaulting to false");

			s_retrieve_msgw = false;
		}

		try {
			// Create an AS400 object using the system name specified by the user.
			AS400 as400 = new AS400(strAs400, strUser, strPass);
			// If a userid and/or password was specified, set them on the
			// AS400 object.
			if (strUser != null)
				as400.setUserId(strUser);
			if (strPass != null)
				as400.setPassword(strPass);
			
			// Get the system name
			SystemStatus systemStatus = new SystemStatus(as400);
			s_systemName = systemStatus.getSystemName().trim();
			
			// Create a job list object. Input parm is the AS400 we want job
			// information from.
			JobList jobList = new JobList(as400);
			// Get a list of jobs running on the server.
			jobList.addJobSelectionCriteria(JobList.SELECTION_PRIMARY_JOB_STATUS_ACTIVE, Boolean.TRUE);
		    jobList.addJobSelectionCriteria(JobList.SELECTION_PRIMARY_JOB_STATUS_JOBQ, Boolean.FALSE);
		    jobList.addJobSelectionCriteria(JobList.SELECTION_PRIMARY_JOB_STATUS_OUTQ, Boolean.FALSE);
		    
		    jobList.addJobAttributeToRetrieve(Job.JOB_QUEUE);
		    jobList.addJobAttributeToRetrieve(Job.JOB_NAME);
		    jobList.addJobAttributeToRetrieve(Job.USER_NAME);
		    jobList.addJobAttributeToRetrieve(Job.JOB_NUMBER);
		    jobList.addJobAttributeToRetrieve(Job.CPU_TIME_USED);
		    jobList.addJobAttributeToRetrieve(Job.JOB_QUEUE_PRIORITY);
		    jobList.addJobAttributeToRetrieve(Job.JOB_STATUS);
		    jobList.addJobAttributeToRetrieve(Job.ACTIVE_JOB_STATUS);
		    jobList.addJobAttributeToRetrieve(Job.SUBSYSTEM);

			Enumeration<?> listOfJobs = jobList.getJobs();
			// For each job in the list print information about the job.
			String strJSONMetrics = "";
			String strJSONHeader = ("{" +
					"\"name\":" +
						'"' +
						s_strNrName +
						'"' +
						"," +
						"\"host\":" +
						'"' +
						strAs400 +
						'"' +
						"," +
						"\"protocol_version\":" +
						'"' +
						s_strNrProtoVersion +
						'"' +
						"," +
						"\"integration_version\":" +
						'"' +
						s_strNrIntVersion +
						'"' +
						"," +
						"\"metrics\":" +
						"[");
			String strJSONFooter = ("]," + "\"inventory\":" + "{" + "}," + "\"events\":" + "[" + "]" + "}");
			System.err.println("JobList: " + jobList.getLength());
			
			while (listOfJobs.hasMoreElements()) {
				String json = getJobInfo((Job) listOfJobs.nextElement(), as400);
				if (json != null) {
					strJSONMetrics += json;
				}
			}
			strJSONMetrics = strJSONMetrics.substring(0, strJSONMetrics.length() - 1);
			System.out.println(strJSONHeader + strJSONMetrics + strJSONFooter);
			
		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
	
	private static String getJobInfo(Job job, AS400 as400) {

		// We have the job name/number/etc. from the list request. Now
		// make a server API call to get the status of the job.

		try {
            
            // Run the program
            byte[] as400Data = retrieveJobInfo(as400, job, "JOBI0200", 256, false);
            if (s_getJobInfo_Result != true)
            {
                // If the program did not run get the list of error messages
                // from the program object and display the messages. The error
                // would be something like program-not-found or not-authorized
                // to the program.
            	if (s_getJobInfo == null) 
            	{
            		System.out.println("The program did not run. Unknown reason.");
            	}
            	return null;//System.exit(-1);
            }
            // Else the program did run.
            else
            {
            	getFormat0200_Data(as400, as400Data);
            }
            
            // Run the program
            as400Data = retrieveJobInfo(as400, job, "JOBI0300", 256, false);
            if (s_getJobInfo_Result != true)
            {
                // If the program did not run get the list of error messages
                // from the program object and display the messages. The error
                // would be something like program-not-found or not-authorized
                // to the program.
            	if (s_getJobInfo == null) 
            	{
            		System.err.println("The program did not run. Unknown reason.");
            	}
            	return null;//System.exit(-1);
            }
            // Else the program did run.
            else
            {
            	getFormat0300_Data(as400, as400Data);
            }

            // Run the program
            as400Data = retrieveJobInfo(as400, job, "JOBI1000", 256, true);
            if (s_getJobInfo_Result == true)
            {
            	//TimeUnit.MILLISECONDS.sleep(s_resetWaitDelay);
            	Thread.sleep(s_resetWaitDelay);
                // Run the program
                as400Data = retrieveJobInfo(as400, job, "JOBI1000", 256, false);
                if (s_getJobInfo_Result != true)
                {
                    // If the program did not run get the list of error messages
                    // from the program object and display the messages. The error
                    // would be something like program-not-found or not-authorized
                    // to the program.
                	if (s_getJobInfo == null) 
                	{
                		System.err.println("The program did not run. Unknown reason.");
                	}
                	return null;//System.exit(-1);
                }
                // Else the program did run.
                else
                {
                	getFormat1000_Data(as400, as400Data);
                }
            }

            // if the active job status states the job is waiting on a msg ("MSGW"), then get msg
            s_jobLogMessage = "";
        	s_jobLogMessageAdditionalInfo = "";
        	s_jobLogMessageHelp = "";

            if (s_retrieve_msgw) {
                try {
    	            if (s_activeJobStatus.trim().equalsIgnoreCase("MSGW")) {
    	                JobLog jlog = new JobLog(as400, job.getName(), job.getUser(), job.getNumber());
    	
    	                // Enumerate the messages in the job log then print them.
    	                Enumeration messageList = jlog.getMessages();
    	
    	                while (messageList.hasMoreElements())
    	                {
    	                   AS400Message message = (AS400Message) messageList.nextElement();
    	                   if (message.getType() == AS400Message.SENDERS_COPY) {
    	                	   s_jobLogMessage = message.getText();
    	                	   message.load();
    	                	   s_jobLogMessageAdditionalInfo = message.getText();
    	                	   s_jobLogMessageHelp = message.getHelp();
    	                   }
    	                   //System.out.println(message.getText());
    	                   //System.out.println("Type: " + message.getType());
    	                }
    	
    	            }
                }
        		catch (Exception e) 
        		{
        			System.err.println("Exception: " + e.getMessage());
        			e.printStackTrace(System.err);
        		}
            }
            
    		return getJsonText();				
		} 
		catch (Exception e) 
		{
			System.err.println("Exception: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return null;
	}

	private static byte[] retrieveJobInfo(AS400 as400, Job job, String format, int bufferLth, boolean resetStats) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
		try
		{
			s_getJobInfo_Result = false; 
			
			AS400Bin4 bin4Converter = new AS400Bin4();
			AS400Text text26Converter = new AS400Text(26, as400);
			AS400Text text16Converter = new AS400Text(16, as400);
			AS400Text text10Converter = new AS400Text(10, as400);
			AS400Text text8Converter = new AS400Text(8, as400);
			AS400Text text6Converter = new AS400Text(6, as400);
			AS400Text text1Converter = new AS400Text(1, as400);
			String jobInfoPgm = "/QSYS.LIB/QUSRJOBI.PGM";

			// Create a program call object
			s_getJobInfo = new ProgramCall(as400);
			
			// The server program we call has five parameters
			ProgramParameter[] parmlist = new ProgramParameter[7];
			
			// The first parm is a byte array that holds the output
			// data. We will allocate a 1k buffer for output data.
			parmlist[0] = new ProgramParameter(1024);
			
			// The second parm is the size of our output data buffer (1K).
			Integer iStatusLength = Integer.valueOf(1024);
			byte[] statusLength = bin4Converter.toBytes(iStatusLength);
			parmlist[1] = new ProgramParameter(statusLength);
			
			// The third parm is the name of the format of the data.
			byte[] statusFormat = text8Converter.toBytes(format);
			parmlist[2] = new ProgramParameter(statusFormat);
			
			// The fourth parm is the job name is format "name user number".
			// Name must be 10 characters, user must be 10 characters and
			// number must be 6 characters. We will use a text converter
			// to do the conversion and padding.
			byte[] jobName = text26Converter.toBytes(job.getName());

			text10Converter.toBytes(job.getUser(), jobName, 10);
			text6Converter.toBytes(job.getNumber(), jobName, 20);
			parmlist[3] = new ProgramParameter(jobName);
			
			// The fifth parameter is job identifier. We will leave this blank.
			byte[] jobID = text16Converter.toBytes("                ");
			parmlist[4] = new ProgramParameter(jobID);
			
	        // Parameter 6 is the error info parameter. It is an input/output
	        // parameter. Add it to the parm list.
	        byte[] errorInfo = new byte[32];
	        parmlist[5] = new ProgramParameter( errorInfo, 0 );

			
			// Parameter 7 Reset performance statistics. We will set this to "0" no reset.
	        
			byte[] resetStatsParm;
			if (resetStats)
			{
				resetStatsParm = text1Converter.toBytes("1");
			}
			else 
			{
				resetStatsParm = text1Converter.toBytes("0");
			}
			parmlist[6] = new ProgramParameter(resetStatsParm);

			
			// Run the program.
			s_getJobInfo_Result = s_getJobInfo.run(jobInfoPgm, parmlist);
			if (s_getJobInfo_Result != true) 
			{
				System.err.println("Failed to run " + jobInfoPgm);
				AS400Message[] messageList = s_getJobInfo.getMessageList();
				for (int i = 0; i < messageList.length; i++) 
				{
					System.err.println(messageList[i]);
				}
				return null;
			} 
			else 
			{

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

	private static void getFormat0200_Data(AS400 as400, byte[] as400Data) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException  {
		AS400Text as400Text;
		Integer anInteger;
        AS400Bin4 as400Int = new AS400Bin4();
        
		as400Text = new AS400Text(10, as400 );
		s_jobName = (String)as400Text.toObject(as400Data, 8); //o:8 l:8

		as400Text = new AS400Text(10, as400 );
		s_jobUser = (String)as400Text.toObject(as400Data, 18); //o:18 l:10
		
		as400Text = new AS400Text(6, as400 );
		s_jobNumber = (String)as400Text.toObject(as400Data, 28); //o:28 l:6
		
        anInteger  = (Integer)as400Int.toObject(as400Data, 80);
        s_jobCPUUsed = (float)anInteger / 1000; //o:80 l:4
		
        anInteger  = (Integer)as400Int.toObject(as400Data, 72);
        s_jobRunPriority =  anInteger.intValue();  //o:72 l:4
		
		as400Text = new AS400Text(10, as400 );
		s_jobStatus = (String)as400Text.toObject(as400Data, 50); //o:50 l:10
		
		as400Text = new AS400Text(4, as400 );
        s_activeJobStatus  = (String)as400Text.toObject(as400Data, 107);
		
		as400Text = new AS400Text(10, as400 );
		s_jobSubsystem = (String)as400Text.toObject(as400Data, 62); //o:62 l:10
	}

	private static void getFormat0300_Data(AS400 as400, byte[] as400Data) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException  {
		AS400Text as400Text;

		as400Text = new AS400Text(10, as400 );
		String jobQueueName = (String)as400Text.toObject(as400Data, 62);

		try 
		{
			if (!jobQueueName.isEmpty())
			{
				as400Text = new AS400Text(10, as400 );
				String jobQueueLibName = (String)as400Text.toObject(as400Data, 72);
				
				s_jobQueue = jobQueueLibName.trim() + "/" + jobQueueName.trim();
			}
		}
		catch (Exception e)
		{
			s_jobQueue = "";
		}
		
		as400Text = new AS400Text(2, as400 );
		s_jobQueuePriority = ((String)as400Text.toObject(as400Data, 82)).trim();
	}

	private static void getFormat1000_Data(AS400 as400, byte[] as400Data) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException  {
		Integer anInteger;
        AS400Bin4 as400Int = new AS400Bin4();
        
        anInteger  = (Integer)as400Int.toObject(as400Data, 104);
        s_jobCPUPct = (float)anInteger.intValue() / 100;
	}
	
	private static String getJsonText() {
		
		String strJSONMetrics =
				"{" +
					"\"event_type\":" +
					'"' +
					"AS400:JobList" +
					'"' +
					"," +
					"\"summary\":" +
					'"' +
					"AS400 Job List Event" +
					'"' +
					"," +
					"\"systemName\":" +
					'"' +
					s_systemName +
					'"' +
					"," +
					"\"jobQueue\":" +
					'"' +
					s_jobQueue + 
					'"' +
					"," +
					"\"jobName\":" +
					'"' +
					s_jobName.trim() + 
					'"' +
					"," +
					"\"jobUser\":" +
					'"' +
					s_jobUser.trim() + 
					'"' +
					"," +
					"\"jobNumber\":" +
					'"' +
					s_jobNumber.trim() + 
					'"' +
					"," +
					"\"jobCPUUsed\":" +
					s_jobCPUUsed + 
					"," +
					"\"jobCPUPct\":" +
					s_jobCPUPct + 
					"," +
					"\"jobQueuePriority\":" +
					'"' +
					s_jobQueuePriority + 
					'"' +
					"," +
					"\"jobRunPriority\":" +
					s_jobRunPriority + 
					"," +
					"\"jobStatus\":" +
					'"' +
					s_jobStatus.trim() + 
					'"' +
					"," +
					"\"activeJobStatus\":" +
					'"' +
					s_activeJobStatus.trim() +
					'"' +
					"," +
					"\"lastJobLogMsg\":" +
					'"' +
					s_jobLogMessage.trim() +
					'"' +
					"," +
					"\"lastJobLogMsgAdditionInfo\":" +
					'"' +
					s_jobLogMessageAdditionalInfo.trim() +
					'"' +
					"," +
					"\"lastJobLogMsgHelp\":" +
					'"' +
					s_jobLogMessageHelp.trim() +
					'"' +
					"," +
					"\"jobSubsystem\":" +
					'"' +
					s_jobSubsystem.trim() + 
					'"' +
					"},";
		return strJSONMetrics;
	}
}
