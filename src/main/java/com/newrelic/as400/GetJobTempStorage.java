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

public class GetJobTempStorage {
    String Version = "\"integration_version\":\"0.2.0\",";

    public GetJobTempStorage() {
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
            rs = stmt.executeQuery("SELECT JOB_NAME, JOB_TYPE, JOB_STATUS, SUBSYSTEM, AUTHORIZATION_NAME, " +
                    "RUN_PRIORITY, TEMPORARY_STORAGE, CPU_TIME, ELAPSED_TIME, THREAD_COUNT " +
                    "FROM TABLE(QSYS2.ACTIVE_JOB_INFO()) X " +
                    "WHERE JOB_TYPE <> 'SYS' AND COALESCE(TEMPORARY_STORAGE, 0) > 0 " +
                    "ORDER BY TEMPORARY_STORAGE DESC FETCH FIRST 25 ROWS ONLY");
            if (rs == null) {
                response.append(Constants.retrieveDataError + " - " + "Cannot retrieve data from server");
                return returnValue;
            }

            StringBuilder jsonMetrics = new StringBuilder();
            jsonMetrics.append("[");

            while (rs.next()) {
                String jobName = rs.getString("JOB_NAME");
                String jobType = rs.getString("JOB_TYPE");
                String jobStatus = rs.getString("JOB_STATUS");
                String subsystem = rs.getString("SUBSYSTEM");
                String authorizationName = rs.getString("AUTHORIZATION_NAME");
                long runPriority = rs.getLong("RUN_PRIORITY");
                long temporaryStorageMB = rs.getLong("TEMPORARY_STORAGE");
                long cpuTimeMs = rs.getLong("CPU_TIME");
                double elapsedTimeSeconds = rs.getDouble("ELAPSED_TIME");
                long threadCount = rs.getLong("THREAD_COUNT");

                jsonMetrics.append("{")
                        .append("\"event_type\":\"AS400:JobTempStorageEvent\",")
                        .append("\"systemName\":\"").append(systemName).append("\",")
                        .append("\"hostName\":\"").append(CommonUtil.getHostName(as400)).append("\",")
                        .append("\"includeInIseriesEntity\":true,")
                        .append("\"jobName\":\"").append(jobName == null ? "" : jobName.trim()).append("\",")
                        .append("\"jobType\":\"").append(jobType == null ? "" : jobType.trim()).append("\",")
                        .append("\"jobStatus\":\"").append(jobStatus == null ? "" : jobStatus.trim()).append("\",")
                        .append("\"subsystem\":\"").append(subsystem == null ? "" : subsystem.trim()).append("\",")
                        .append("\"authorizationName\":\"").append(authorizationName == null ? "" : authorizationName.trim()).append("\",")
                        .append("\"runPriority\":").append(runPriority).append(",")
                        .append("\"temporaryStorageMB\":").append(temporaryStorageMB).append(",")
                        .append("\"cpuTimeMs\":").append(cpuTimeMs).append(",")
                        .append("\"elapsedTimeSeconds\":").append(elapsedTimeSeconds).append(",")
                        .append("\"threadCount\":").append(threadCount)
                        .append("},");
            }

            // Remove the last comma and close the JSON array
            if (jsonMetrics.length() > 1) {
                jsonMetrics.setLength(jsonMetrics.length() - 1);
            }
            jsonMetrics.append("]");

            response.append("{")
                    .append("\"name\":\"com.newrelic.as400-job-temp-storage\",")
                    .append("\"protocol_version\":\"1\",")
                    .append(Version)
                    .append("\"metrics\":").append(jsonMetrics.toString()).append(",")
                    .append("\"inventory\":{},")
                    .append("\"events\":[]")
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

        GetJobTempStorage jobTempStorage = new GetJobTempStorage();
        Map<String, String> arguments = new HashMap<>();
        arguments.put("-U", strUser);
        arguments.put("-P", strPass);
        arguments.put("-SSL", "false");

        StringBuffer response = new StringBuffer();
        jobTempStorage.execute(as400, arguments, response);

        System.out.println(response.toString());
    }
}
