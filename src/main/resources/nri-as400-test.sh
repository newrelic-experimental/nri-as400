#!/bin/sh

# Test script for as400 OHIs

# Needed for all tests
export AS400HOST=pub400.com
export PASSWD=user0465
export USERID=USER0465

# Only needed if testing message-queue
export MSGQUEUE=/QSYS.LIB/QSYSOPR.MSGQ
export INSTANCE=pub400.QSYSOPR

execute_class () {
  start_time=`date +%s`
  /usr/bin/java -cp ./nri-as400.jar -Xdiag -Dcom.ibm.as400.access.AS400.guiAvailable=false com.newrelic.as400.$1
  echo Time to execute $2: $(expr `date +%s` - $start_time) seconds
}

usage() {
    echo "Usage: nri-as400-test.sh [job-list|message-queue|system-status|memory-status|disk-usage]"
    exit 1
}

case "$1" in
    "")
      usage
      ;;
    "job-list")
      execute_class "GetJobList" $1
      ;;
    "message-queue")
      execute_class "GetMsgQueue" $1
      ;;
    "system-status")
      execute_class "GetSystemStatus" $1
      ;;
    "memory-status")
      execute_class "GetMemoryStatus" $1
      ;;
    "disk-usage")
      execute_class "GetDiskUsage" $1
      ;;
    *)
      usage
      ;;
esac

exit 0
