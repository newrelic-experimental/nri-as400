# nri-as400

iSeries / AS400 monitoring solution for New Relic Infrastructure. Actually split into 4 separate OHIs, all described below.

## Prerequisites

- New Relic Infrastructure Agent installed on a Linux host
- Linux host must be able to remotely address the iSeries/AS400 server(s) in question

## Installation
- Unzip/gunzip nri-as400 package on host where NRI Agent is installed
- Test your credentials and parameters with [`nri-as400-test.sh`](#testing-with-nri-as400-testsh)
- Edit `nri-as400/nri-as400-config.yml`, configuring instances of the 4 OHIs outlined below as needed.
  - [as400-job-list - iSeries Active Jobs](#config-as400-job-list)
  - [as400-message-queue - iSeries Message Queues](#config-as400-message-queue)
  - [as400-system-status - iSeries Server KPIs](#config-as400-system-status)
  - [as400-memory-status - iSeries Server Memory Usage KPIs](#config-as400-memory-status)
- run `nri-as400/install_linux.sh`

## Configuration

Each section below details the possible settings for an instance of that OHI. All of these settings should be in the `instances:` stanza of `nri-as400-config.yml`.

### as400-job-list - iSeries Active Jobs <a id="config-as400-job-list"></a>
```yaml
instances:
  - name: pub400_jobs
    command: job-list
    arguments:
      as400host: pub400.com
      userid: USER0465
      passwd: user0465
      reset_wait_delay: 100
```

* `name`: The name of the instance, usually the host and "_jobs"
* `as400host`: The iSeries host.
* `userid`: The user that has access to the message queue.
* `passwd`: The password for the user.
* `reset_wait_delay`: The delay interval (in ms) between resetting stats and capturing them - optional, default to 100ms 
* `retrieve_msgw`: Retrieves the outstanding last message for jobs with an active job status of MSGW. Optional, default to false

#### Notes
* This OHI can be configured to pull job data from multiple iSeries servers
* **Recommended**: Depending on the server, this could take over 60 seconds to complete. [Test using `nri-as400-test.sh job-list`](#testing-with-nri-as400-testsh) and note the time it takes to complete. If longer than 60 seconds, increase the `interval` in `nri-as400-definition.yml` to be at least as long as that time.
* The attributes collected for each job are a subset of those that can be included, and it would not be difficult to extend the agent to collect additional attributes.

### as400-message-queue - iSeries Message Queues <a id="config-as400-message-queue"></a>

```yaml
instances:
  - name: pub400_queue_QSYSOPR
    command: message-queue
    arguments:
      instance: pub400.QSYSOPR
      as400host: pub400.com
      msgqueue: /QSYS.LIB/QSYSOPR.MSGQ
      userid: USER0465
      passwd: user0465
```

* `name`: The name of the instance, an instance being a combination of iSeries host and message queue.
* `as400host`: The iSeries host.
* `userid`: The user that has access to the message queue.
* `passwd`: The password for the user.
* `instance`: Can be the same as `name`, used to build / maintain the checkpoint file.
* `msgqueue`: The iSeries message queue to be monitored.

#### Notes

* This OHI can be configured to monitor multiple Messages Queues on Multiple iSeries servers.
* The agent on startup will read and post the last message in the queue, subsequent executions will read from the last read message and process only new messages.
* There are checkpoint files which contain the binary value of the 4 byte message key that was last processed in the OHI working directory (usually /var/db/newrelic-infra/custom-integrations). Deleting the checkpoint file will force the agent to only read the latest message, this might be helpful / required in the message queue does not reset after an IPL (like QSYSOPR).
* Execution interval should take into account the verbosity of the queue, however in general 30 to 60 seconds should be fine.

### as400-system-status - iSeries Server KPIs <a id="config-as400-system-status"></a>

```yaml
instances:
  - name: pub400_status
    command: system-status
    arguments:
      as400host: pub400.com
      userid: USER0465
      passwd: user0465
```

* `name`: The name of the instance, usually the host and "_status"
* `as400host`: The iSeries host.
* `userid`: The user that has access to the message queue.
* `passwd`: The password for the user.

#### Notes
* This OHI can be configured to pull server KPIs from multiple iSeries servers.
* Execution interval should be fine at 30 to 60 seconds.

### as400-memory-status - iSeries Server Memory Usage KPIs <a id="config-as400-memory-status"></a>

```yaml
instances:
  - name: pub400_status
    command: memory-status
    arguments:
      as400host: pub400.com
      userid: USER0465
      passwd: user0465
```

* `name`: The name of the instance, usually the host and "_status"
* `as400host`: The iSeries host.
* `userid`: The user that has access to the message queue.
* `passwd`: The password for the user.

#### Notes
* This OHI can be configured to pull server memory usage KPIs from multiple iSeries servers.
* Execution interval should be fine at 30 to 60 seconds.

## Testing with `nri-as400-test.sh` <a id="testing-with-nri-as400-testsh"></a>
This collection of OHIs comes with `nri-as400-test.sh`, a shell script to verify:
* connectivity to AS400 host
* credentials and other parameters
* data returned by the OHI instances

#### Usage
* Edit `nri-as400-test.sh`, setting the environment variables at the top to the settings you will use in this configuration.
* If not executable, run `chmod +x nri-as400-test.sh`
* Run `./nri-as400-test.sh [job-list|message-queue|system-status]`

## Data Types

### as400-job-list
Event Type: `AS400:JobList`

Attributes:
- `event_type` - Required for all OHI events.
- `summary` - Summary of the event. Optional for OHI events.
- `systemName` - iSeries name
- `jobQueue` - The job queue that the job was submitted to.
- `jobName` - The executing job name.
- `jobUser` - The user which submitted the job.
- `jobNumber` - The unique job number assigned to the job.
- `jobCPUUsed` - Amount of CPU used by the job.
- `jobCPUPct` - Amount of CPU % used by the job during the capturiing interval.
- `jobQueuePriority` - Current job queue priority assigned to the job.
- `jobRunPriority` - Current job run priority assigned to the job.
- `jobStatus` - Current status of the active job - will always be *ACTIVE.
- `activeJobStatus` - Current active status of the job.
- `lastJobLogMsg` - Last message in the job log - retrieved when active job status is MSGW and parm retrieve_msgw is true.
- `lastJobLogMsgAdditionInfo` - Last additional info message in the job log - retrieved when active job status is MSGW and parm retrieve_msgw is true.
- `lastJobLogMsgHelp` - Last help message in the job log - retrieved when active job status is MSGW and parm retrieve_msgw is true.
- `jobSubsystem` - The job subsystem that the job is currently executing in.

### as400-message-queue
Event Type: `AS400:MessageQueueEvent`

Attributes:
- `event_type` - Required for all OHI events.
- `summary` - Summary of the event. Optional for OHI events.
- `host` - The iSeries host that the message queue resides on.
- `queue` - The iSeries message queue being polled.
- `messageID` - The 4 byte message unique message identifier.
- `job` - The name of the job which produced the message.
- `jobNumber` - The job number of the job which produced the message
- `type` - The message type.
- `program` - The program from which the message originated.
- `severity` - The numeric message severity 00 through 99.
- `replyStatus` - If the message requires a reply from an operator, the status of that reply.
- `user` - The user associated with the job that originated the message.
- `message` - The message text that appears on the operator console.
- `messageHelp` - The message help text (if any) associated with the message.

### as400-system-status
Event Type: `AS400:SystemStatusEvent`

Attributes:
- `event_type` - Required for all OHI events.
- `numberActiveJobsInSystem` - Number of jobs in the system that are currently active and running.
- `activeThreadsInSystem` - Number of active threads in the system.
- `batchJobsEndedWithPrinterOutputWaitingToPrint` - Number of jobs that have ended, but are currently waiting for output to spool to a printer.
- `numberOfBatchJobsEnding` - Number of batch jobs ending.
- `numberOfBatchJobsHeldOnQueue` - Number of batch jobs that have been held in the job queue.
- `nummberOfBatchJobsOnUnassignedQueues` - Number of batch jobs on unassigned queues.
- `numberOfBatchJobsRunning` - Number of batch (versus interactive) jobs running on the system.
- `numberOfBatchJobsWaitingForMessage` - Number of batch jobs that have produced a message requiring a reply to a message queue.
- `numberOfBatchJobsWaitingToRunOrAlreadyScheduled` - Number of batch jobs waiting execution.
- `currentProcessingCapacity` - Current processing capacity.
- `currentUnprotectedStorageUsed` - Unprotected storage currently in use.
- `dateTimeStatusGathered` - Date / Time of collection.
- `numberOfJobsInSystem` - Number of total jobs currently in the system.
- `mainStorageSize` - Main storage capacity.
- `maxJobsInSystem` - Maximum number of jobs allowed in the system.
- `maxUnprotectedStorageUsed` - Maximum unprotected storage units allowed in the system.
- `numberOfPartitions` - Number of partitions active on the system.
- `numberOfProcessors` - Number of processors enabled on the system.
- `partitionIdentifier` - Logical partition ID.
- `percentCurrentInteractivePerformance` - Percent of current interactive performance.
- `percentDBCapability` - Percent of database capability.
- `percentPermanent4GBSegmentUsed` - Percent of permanent 4GB segments in use.
- `percentProcessingUnitUsed` - Percent of processing units currently in use.
- `percentSharedProcessorPoolUsed` - Percent of shared processing pool currently in use.
- `percentSystemASPUsed` - Percent of Auxiliary storage pools in use.
- `percentTemporary256MBSegmentsUsed` - Percent of temporary 256MB segments in use.
- `percentTemporary4GBSegmentsUsed` - Percent of temporary 4GB segments in use.
- `percentTemporaryAddresses` - Percent of temporary addresses in use.
- `percentUncappedCPUCapacityUsed` - Percent of uncapped SPU capacity in use.
- `poolsNumber` - Number of pools.
- `processorSharingAttribute` - Processor sharing disposition.
- `restrictedStateFlag` - Restricted state flag setting.
- `systemASP` - Total size of auxiliary storage pool.
- `systemName` - iSeries name
- `totalAuxiliaryStorage` - Total size of auxiliary storage.
- `currentUsersSignedOn` - Total number of users logged into the system.
- `usersSignedOffWithPrinterOutputWaitingToPrint` - users signed off with output pending print.
- `usersSuspendedBySystemRequest` - Users suspended by a system request

### as400-memory-status
Event Type: `AS400:MemoryStatusEvent`

Attributes:
- `event_type` - Required for all OHI events.
- `eventInstanceId` - A unique ID correlating the variaous storage pools metrics captured together.
- `systemName` - iSeries name.
- `dateTimeStatusGathered` - Date / Time of collection.
- `mainStorageSize` - The amount of main storage, in kilobytes, in the system..
- `minimumMachinePoolSize` - The minimum size, in kilobytes, for the machine pool.
- `minimumBasePoolSize` - The minimum size, in kilobytes, for the base pool.
- `numberOfPools` - The number of pools allocated when the information was gathered.
- `poolName` - The name of this storage pool.
- `subsystemName` - The name of the system where the statistics were collected.
- `susbsystemLibraryName` - The subsystem with which this storage pool is associated.
- `pagingOption` - Whether the system will dynamically adjust the paging characteristics of the storage pool for optimum performance. 
- `description` - The description of the shared pool.
- `status` - The status of the pool: 0=Active 1=Inactive.
- `systemPools` - The system-related pool identifier for each of the system storage pools that currently has main storage allocated to it.
- `poolSize` - The amount of main storage, in kilobytes, in the pool. 
- `maximumActiveThreads` - The maximum number of threads that can be active in the pool at any one time.
- `databaseFaults` -  The rate (in tenths), shown in page faults per second, of database page faults against pages containing either database data or access paths. A page fault is a program notification that occurs when a page that is marked as not in main storage is referred to by an active program.
- `databasePages` - he rate (in tenths), in pages per second, at which database pages are brought into the storage pool. A page is a 4096-byte block of information that is transferable between auxiliary storage and main storage.
- `nondatabaseFaults` - The rate (in tenths), in page faults per second, of nondatabase page faults against pages other than those designated as database pages.
- `nondatabasePages` - The rate (in tenths), in pages per second, at which nondatabase pages are brought into the storage pool.
- `activeToWait` - The rate (in tenths), in transitions per minute, of transitions of threads from an active condition to a waiting condition.
- `waitToIneligible` - The rate (in tenths), in transitions per minute, of transitions of threads from a waiting condition to an ineligible condition.
- `activeToIneligible` - The rate (in tenths), in transitions per minute, of transitions of threads from an active condition to an ineligible condition.
- `definedSize` - The size of the pool, in kilobytes, as defined in the shared pool, subsystem description, or system value QMCHPOOL.
- `currentThreads` - The number of threads currently using the pool's activity level.
- `currentIneligibleThreads` - The number of ineligible threads in the pool's activity level.
