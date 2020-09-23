//  ----------------------------------------------------------------------
//
//  Name: GetSystemStatus
//
//  ----------------------------------------------------------------------
//  Modification Log
//  ----------------------------------------------------------------------
//  2/9/2019 - Initial creation - MJ
//  ----------------------------------------------------------------------
package com.newrelic.as400;

import java.io.*;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.SystemStatus;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;

public class GetSystemStatus {

	public static void main(String[] args) throws AS400SecurityException, ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
		String strAs400 = System.getenv("AS400HOST");
		String strUser = System.getenv("USERID");
		String strPass = System.getenv("PASSWD");
		String strNrName = "com.newrelic.as400-system-status";
		String strNrEventType = "AS400:SystemStatusEvent";
		String strNrProtoVersion = "1";
		String strNrIntVersion = "0.1.0";
		String strJSONMetrics = "";
		String strJSONHeader = ("{" + "\"name\":" + '"' + strNrName + '"' + "," + "\"protocol_version\":" + '"' + strNrProtoVersion + '"' + "," + "\"integration_version\":" + '"' + strNrIntVersion + '"' + "," + "\"metrics\":" + "[");
		String strJSONFooter = ("]," + "\"inventory\":" + "{" + "}," + "\"events\":" + "[" + "]" + "}");
		AS400 as400 = new AS400(strAs400, strUser, strPass);
		SystemStatus systemStatus = new SystemStatus(as400);
		// String GCPU = Integer.toString(systemStatus.getCPUUsed());
		strJSONMetrics = strJSONMetrics +
				"{" +
					"\"event_type\":" +
					'"' +
					strNrEventType +
					'"' +
					"," +
					"\"numberActiveJobsInSystem\":" +
					systemStatus.getActiveJobsInSystem() +
					"," +
					"\"activeThreadsInSystem\":" +
					systemStatus.getActiveThreadsInSystem() +
					"," +
					"\"batchJobsEndedWithPrinterOutputWaitingToPrint\":" +
					systemStatus.getBatchJobsEndedWithPrinterOutputWaitingToPrint() +
					"," +
					"\"numberOfBatchJobsEnding\":" +
					systemStatus.getBatchJobsEnding() +
					"," +
					"\"numberOfBatchJobsHeldOnQueue\":" +
					systemStatus.getBatchJobsHeldOnJobQueue() +
					"," +
					"\"nummberOfBatchJobsOnUnassignedQueues\":" +
					systemStatus.getBatchJobsOnUnassignedJobQueue() +
					"," +
					"\"numberOfBatchJobsRunning\":" +
					systemStatus.getBatchJobsRunning() +
					"," +
					"\"numberOfBatchJobsWaitingForMessage\":" +
					systemStatus.getBatchJobsWaitingForMessage() +
					"," +
					"\"numberOfBatchJobsWaitingToRunOrAlreadyScheduled\":" +
					systemStatus.getBatchJobsWaitingToRunOrAlreadyScheduled() +
					"," +
					"\"currentProcessingCapacity\":" +
					systemStatus.getCurrentProcessingCapacity() +
					"," +
					"\"currentUnprotectedStorageUsed\":" +
					systemStatus.getCurrentUnprotectedStorageUsed() +
					"," +
					"\"dateTimeStatusGathered\":" +
					'"' +
					systemStatus.getDateAndTimeStatusGathered() +
					'"' +
					"," +
					"\"numberOfJobsInSystem\":" +
					systemStatus.getJobsInSystem() +
					"," +
					"\"mainStorageSize\":" +
					systemStatus.getMainStorageSize() +
					"," +
					"\"maxJobsInSystem\":" +
					systemStatus.getMaximumJobsInSystem() +
					"," +
					"\"maxUnprotectedStorageUsed\":" +
					systemStatus.getMaximumUnprotectedStorageUsed() +
					"," +
					"\"numberOfPArtitions\":" +
					systemStatus.getNumberOfPartitions() +
					"," +
					"\"numberOfProcessors\":" +
					systemStatus.getNumberOfProcessors() +
					"," +
					"\"partitionIdentifier\":" +
					'"' +
					systemStatus.getPartitionIdentifier() +
					'"' +
					"," +
					"\"percentCurrentInteractivePerformance\":" +
					systemStatus.getPercentCurrentInteractivePerformance() +
					"," +
					"\"percentDBCapability\":" +
					systemStatus.getPercentDBCapability() +
					"," +
					"\"percentPermanent4GBSegmentUsed\":" +
					systemStatus.getPercentPermanent256MBSegmentsUsed() +
					"," +
					"\"percentProcessingUnitUsed\":" +
					systemStatus.getPercentPermanentAddresses() +
					"," +
					"\"percentSharedProcessorPoolUsed\":" +
					systemStatus.getPercentSharedProcessorPoolUsed() +
					"," +
					"\"percentSystemASPUsed\":" +
					systemStatus.getPercentSystemASPUsed() +
					"," +
					"\"percentTemporary256MBSegmentsUsed\":" +
					systemStatus.getPercentTemporary256MBSegmentsUsed() +
					"," +
					"\"percentTemporary4GBSegmentsUsed\":" +
					systemStatus.getPercentTemporary4GBSegmentsUsed() +
					"," +
					"\"percentTemporaryAddresses\":" +
					systemStatus.getPercentTemporaryAddresses() +
					"," +
					"\"percentUncappedCPUCapacityUsed\":" +
					systemStatus.getPercentUncappedCPUCapacityUsed() +
					"," +
					"\"poolsNumber\":" +
					systemStatus.getPoolsNumber() +
					"," +
					"\"processorSharingAttribute\":" +
					systemStatus.getProcessorSharingAttribute() +
					"," +
					"\"restrictedStateFlag\":" +
					'"' +
					systemStatus.getRestrictedStateFlag() +
					'"' +
					"," +
					"\"systemASP\":" +
					systemStatus.getSystemASP() +
					"," +
					"\"systemName\":" +
					'"' +
					systemStatus.getSystemName() +
					'"' +
					"," +
					"\"totalAuxiliaryStorage\":" +
					systemStatus.getTotalAuxiliaryStorage() +
					"," +
					"\"currentUsersSignedOn\":" +
					systemStatus.getUsersCurrentSignedOn() +
					"," +
					"\"usersSignedOffWithPrinterOutputWaitingToPrint\":" +
					systemStatus.getUsersSignedOffWithPrinterOutputWaitingToPrint() +
					"," +
					"\"usersSuspendedBySystemRequest\":" +
					systemStatus.getUsersSuspendedByGroupJobs() +
					"}";
		// strJSONMetrics = strJSONMetrics.substring(0, strJSONMetrics.length() -1);
		System.out.println(strJSONHeader + strJSONMetrics + strJSONFooter);
	}
}
