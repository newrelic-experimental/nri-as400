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
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;

public class GetJobList {

	public static void main(String[] args) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
		// Set up AS400 object parms. The first is the system name and must
		// be specified by the user. The second and third are optional. They
		// are the userid and password. Convert the userid and password
		// to uppercase before setting them on the AS400 object.
		String strAs400 = System.getenv("AS400HOST");
		String strUser = System.getenv("USERID");
		String strPass = System.getenv("PASSWD");
		String strNrName = "com.newrelic.as400-job-list";
		String strNrIntVersion = "0.1.0";
		String strNrProtoVersion = "1";
		try {
			// Create an AS400 object using the system name specified by the user.
			AS400 as400 = new AS400(strAs400, strUser, strPass);
			// If a userid and/or password was specified, set them on the
			// AS400 object.
			if (strUser != null)
				as400.setUserId(strUser);
			if (strPass != null)
				as400.setPassword(strPass);
			// Create a job list object. Input parm is the AS400 we want job
			// information from.
			JobList jobList = new JobList(as400);
			// Get a list of jobs running on the server.
			jobList.addJobSelectionCriteria(JobList.SELECTION_PRIMARY_JOB_STATUS_ACTIVE, Boolean.TRUE);
			Enumeration<?> listOfJobs = jobList.getJobs();
			// For each job in the list print information about the job.
			String strJSONMetrics = "";
			String strJSONHeader = ("{" +
					"\"name\":" +
						'"' +
						strNrName +
						'"' +
						"," +
						"\"host\":" +
						'"' +
						strAs400 +
						'"' +
						"," +
						"\"protocol_version\":" +
						'"' +
						strNrProtoVersion +
						'"' +
						"," +
						"\"integration_version\":" +
						'"' +
						strNrIntVersion +
						'"' +
						"," +
						"\"metrics\":" +
						"[");
			String strJSONFooter = ("]," + "\"inventory\":" + "{" + "}," + "\"events\":" + "[" + "]" + "}");
			System.err.println("JobList: " + jobList.getLength());
			while (listOfJobs.hasMoreElements()) {
				strJSONMetrics += getJobInfo((Job) listOfJobs.nextElement(), as400);
			}
			strJSONMetrics = strJSONMetrics.substring(0, strJSONMetrics.length() - 1);
			System.out.println(strJSONHeader + strJSONMetrics + strJSONFooter);
		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}

	private static String getJobInfo(Job job, AS400 as400) {
		String strJSONMetrics = "";
		// Create the various converters we need
		AS400Bin4 bin4Converter = new AS400Bin4();
		AS400Text text26Converter = new AS400Text(26, as400);
		AS400Text text16Converter = new AS400Text(16, as400);
		AS400Text text10Converter = new AS400Text(10, as400);
		AS400Text text8Converter = new AS400Text(8, as400);
		AS400Text text6Converter = new AS400Text(6, as400);
		String jobInfoPgm = "/QSYS.LIB/QUSRJOBI.PGM";

		new AS400Text(4, as400);
		// We have the job name/number/etc. from the list request. Now
		// make a server API call to get the status of the job.
		try {
			// Create a program call object
			ProgramCall pgm = new ProgramCall(as400);
			// The server program we call has five parameters
			ProgramParameter[] parmlist = new ProgramParameter[5];
			// The first parm is a byte array that holds the output
			// data. We will allocate a 1k buffer for output data.
			parmlist[0] = new ProgramParameter(1024);
			// The second parm is the size of our output data buffer (1K).
			Integer iStatusLength = new Integer(1024);
			byte[] statusLength = bin4Converter.toBytes(iStatusLength);
			parmlist[1] = new ProgramParameter(statusLength);
			// The third parm is the name of the format of the data.
			// We will use format JOBI0200 because it has job status.
			byte[] statusFormat = text8Converter.toBytes("JOBI0200");
			parmlist[2] = new ProgramParameter(statusFormat);
			// The fourth parm is the job name is format "name user number".
			// Name must be 10 characters, user must be 10 characters and
			// number must be 6 characters. We will use a text converter
			// to do the conversion and padding.
			byte[] jobName = text26Converter.toBytes(job.getName());

			text10Converter.toBytes(job.getUser(), jobName, 10);
			text6Converter.toBytes(job.getNumber(), jobName, 20);
			parmlist[3] = new ProgramParameter(jobName);
			// The last parameter is job identifier. We will leave this blank.
			byte[] jobID = text16Converter.toBytes("                ");
			parmlist[4] = new ProgramParameter(jobID);
			// Run the program.

			if (pgm.run(jobInfoPgm, parmlist) != true) {
				System.err.println("Failed to run " + jobInfoPgm);
				AS400Message[] messageList = pgm.getMessageList();
				for (int i = 0; i < messageList.length; i++) {
					System.err.println(messageList[i]);
				}
			} else {
				strJSONMetrics =
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
							"\"jobQueue\":" +
							'"' +
							job.getQueue() +
							'"' +
							"," +
							"\"jobName\":" +
							'"' +
							job.getName() +
							'"' +
							"," +
							"\"jobUser\":" +
							'"' +
							job.getUser() +
							'"' +
							"," +
							"\"jobNumber\":" +
							'"' +
							job.getNumber() +
							'"' +
							"," +
							"\"jobCPUUsed\":" +
							'"' +
							job.getCPUUsed() +
							'"' +
							"," +
							"\"jobQueuePriority\":" +
							'"' +
							job.getQueuePriority() +
							'"' +
							"," +
							"\"jobRunPriority\":" +
							'"' +
							job.getRunPriority() +
							'"' +
							"," +
							"\"jobStatus\":" +
							'"' +
							job.getStatus() +
							'"' +
							"," +
							"\"jobSubsystem\":" +
							'"' +
							job.getSubsystem() +
							'"' +
							"},";
				// lse the program worked. Output the status followed by
				// the jobName.user.jobID
				// byte[] as400Data = parmlist[0].getOutputData();
			}
		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return strJSONMetrics;
	}
}
