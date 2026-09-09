package com.newrelic.as400;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.SystemStatus;
import com.newrelic.labs.utils.JDBCConnection;
import com.newrelic.labs.utils.Constants;
import com.newrelic.labs.utils.CommonUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class GetJobQueueInfo {
    String Version = "\"integration_version\":\"0.2.0\",";

    public GetJobQueueInfo() {
    }

    public int execute(AS400 as400, Map<String, String> args, StringBuffer response) {
        int returnValue = Constants.UNKNOWN;
        Statement stmt = null;
        ResultSet rs = null;

        Connection connection = null;
        try {
            String systemName = new SystemStatus(as400).getSystemName().trim();
            JDBCConnection JDBCConn = new JDBCConnection();
            connection = JDBCConn.getJDBCConnection(as400.getSystemName(), args.get("-U"), args.get("-P"), args.get("-SSL"));
            if (connection == null) {
                response.append(Constants.retrieveDataError + " - " + "Cannot get the JDBC connection");
                return returnValue;
            }
            stmt = connection.createStatement();
            rs = stmt.executeQuery("SELECT JOB_QUEUE_NAME, JOB_QUEUE_LIBRARY, JOB_QUEUE_STATUS, SUBSYSTEM_NAME, " +
                    "SUBSYSTEM_LIBRARY_NAME, MAXIMUM_ACTIVE_JOBS, ACTIVE_JOBS, HELD_JOBS, RELEASED_JOBS, " +
                    "SCHEDULED_JOBS, NUMBER_OF_JOBS FROM QSYS2.JOB_QUEUE_INFO");
            if (rs == null) {
                response.append(Constants.retrieveDataError + " - " + "Cannot retrieve data from server");
                return returnValue;
            }

            StringBuilder jsonMetrics = new StringBuilder();
            jsonMetrics.append("[");

            while (rs.next()) {
                String jobQueueName = rs.getString("JOB_QUEUE_NAME");
                String jobQueueLibrary = rs.getString("JOB_QUEUE_LIBRARY");
                String jobQueueStatus = rs.getString("JOB_QUEUE_STATUS");
                String subsystemName = rs.getString("SUBSYSTEM_NAME");
                String subsystemLibraryName = rs.getString("SUBSYSTEM_LIBRARY_NAME");
                long maximumActiveJobs = rs.getLong("MAXIMUM_ACTIVE_JOBS");
                long activeJobs = rs.getLong("ACTIVE_JOBS");
                long heldJobs = rs.getLong("HELD_JOBS");
                long releasedJobs = rs.getLong("RELEASED_JOBS");
                long scheduledJobs = rs.getLong("SCHEDULED_JOBS");
                long numberOfJobs = rs.getLong("NUMBER_OF_JOBS");

                jsonMetrics.append("{")
                        .append("\"event_type\":\"AS400:JobQueueEvent\",")
                        .append("\"jobQueueName\":\"").append(jobQueueName == null ? "" : jobQueueName.trim()).append("\",")
                        .append("\"jobQueueLibrary\":\"").append(jobQueueLibrary == null ? "" : jobQueueLibrary.trim()).append("\",")
                        .append("\"jobQueueStatus\":\"").append(jobQueueStatus == null ? "" : jobQueueStatus.trim()).append("\",")
                        .append("\"subsystemName\":\"").append(subsystemName == null ? "" : subsystemName.trim()).append("\",")
                        .append("\"subsystemLibraryName\":\"").append(subsystemLibraryName == null ? "" : subsystemLibraryName.trim()).append("\",")
                        .append("\"maximumActiveJobs\":").append(maximumActiveJobs).append(",")
                        .append("\"activeJobs\":").append(activeJobs).append(",")
                        .append("\"heldJobs\":").append(heldJobs).append(",")
                        .append("\"releasedJobs\":").append(releasedJobs).append(",")
                        .append("\"scheduledJobs\":").append(scheduledJobs).append(",")
                        .append("\"numberOfJobs\":").append(numberOfJobs)
                        .append("},");
            }

            // Remove the last comma and close the JSON array
            if (jsonMetrics.length() > 1) {
                jsonMetrics.setLength(jsonMetrics.length() - 1);
            }
            jsonMetrics.append("]");

            response.append("{")
                    .append("\"name\":\"com.newrelic.as400-job-queue-info\",")
                    .append("\"protocol_version\":\"3\",")
                    .append(Version)
                    .append("\"data\":[{")
                    .append("\"entity\":{\"name\":\"").append(systemName).append("\",\"type\":\"as400-system\"},")
                    .append("\"metrics\":").append(jsonMetrics.toString()).append(",")
                    .append("\"inventory\":{},")
                    .append("\"events\":[]")
                    .append("}]")
                    .append("}");

            returnValue = Constants.OK;
            return returnValue;
        } catch (Exception e) {
            response.setLength(0);
            response.append(Constants.retrieveDataException + " - " + e.toString());
            CommonUtil.printStack(e.getStackTrace(), response);
            CommonUtil.logError(args.get("-H"), this.getClass().getName(), e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                response.append(Constants.retrieveDataException + " - " + e.toString());
                e.printStackTrace();
            }
        }
        return returnValue;
    }

    public static void main(String[] args) {
        String strAs400 = System.getenv("AS400HOST");
        String strUser = System.getenv("USERID");
        String strPass = System.getenv("PASSWD");
        AS400 as400 = new AS400(strAs400, strUser, strPass);

        GetJobQueueInfo jobQueueInfo = new GetJobQueueInfo();
        Map<String, String> arguments = new HashMap<>();
        arguments.put("-U", strUser);
        arguments.put("-P", strPass);
        arguments.put("-SSL", "false");

        StringBuffer response = new StringBuffer();
        jobQueueInfo.execute(as400, arguments, response);

        System.out.println(response.toString());
    }
}
