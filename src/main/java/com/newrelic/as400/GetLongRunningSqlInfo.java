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
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class GetLongRunningSqlInfo {
    String Version = "\"integration_version\":\"0.2.0\",";

    public GetLongRunningSqlInfo() {
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
            rs = stmt.executeQuery("SELECT JOB_NAME, JOB_STATUS, SUBSYSTEM, ELAPSED_TIME, SQL_STATEMENT_TEXT, " +
                    "SQL_STATEMENT_STATUS, SQL_STATEMENT_START_TIMESTAMP, DATABASE_LOCK_WAIT_TIME, " +
                    "NON_DATABASE_LOCK_WAIT_TIME " +
                    "FROM TABLE(QSYS2.ACTIVE_JOB_INFO(DETAILED_INFO => 'FULL')) X " +
                    "WHERE SQL_STATEMENT_STATUS = 'ACTIVE' " +
                    "OR JOB_STATUS IN ('LCKW','MTXW','SEMW','LSPW','MSGW') " +
                    "ORDER BY ELAPSED_TIME DESC FETCH FIRST 25 ROWS ONLY");
            if (rs == null) {
                response.append(Constants.retrieveDataError + " - " + "Cannot retrieve data from server");
                return returnValue;
            }

            StringBuilder jsonMetrics = new StringBuilder();
            jsonMetrics.append("[");

            while (rs.next()) {
                String jobName = rs.getString("JOB_NAME");
                String jobStatus = rs.getString("JOB_STATUS");
                String subsystem = rs.getString("SUBSYSTEM");
                // ELAPSED_TIME on QSYS2.ACTIVE_JOB_INFO is one of IBM's "elapsed statistics" columns:
                // it measures time since this job's stats were last explicitly reset, not how long
                // the job has been stuck. Since this query never resets stats, it reads 0 for the
                // (typical) job that's never had a reset. lockWaitTimeMs below is the real signal.
                double elapsedTimeSeconds = rs.getDouble("ELAPSED_TIME");
                String sqlStatementText = rs.getString("SQL_STATEMENT_TEXT");
                String sqlStatementStatus = rs.getString("SQL_STATEMENT_STATUS");
                Timestamp sqlStatementStartTimestamp = rs.getTimestamp("SQL_STATEMENT_START_TIMESTAMP");
                long databaseLockWaitTimeMs = rs.getLong("DATABASE_LOCK_WAIT_TIME");
                long nonDatabaseLockWaitTimeMs = rs.getLong("NON_DATABASE_LOCK_WAIT_TIME");
                long lockWaitTimeMs = Math.max(databaseLockWaitTimeMs, nonDatabaseLockWaitTimeMs);

                jsonMetrics.append("{")
                        .append("\"event_type\":\"AS400:LongRunningSqlEvent\",")
                        .append("\"systemName\":\"").append(systemName).append("\",")
                        .append("\"hostName\":\"").append(CommonUtil.getHostName(as400)).append("\",")
                        .append("\"includeInIseriesEntity\":true,")
                        .append("\"jobName\":\"").append(jsonEscape(jobName == null ? "" : jobName.trim())).append("\",")
                        .append("\"jobStatus\":\"").append(jsonEscape(jobStatus == null ? "" : jobStatus.trim())).append("\",")
                        .append("\"subsystem\":\"").append(jsonEscape(subsystem == null ? "" : subsystem.trim())).append("\",")
                        .append("\"elapsedTimeSeconds\":").append(elapsedTimeSeconds).append(",")
                        .append("\"sqlStatementText\":\"").append(jsonEscape(sqlStatementText == null ? "" : sqlStatementText)).append("\",")
                        .append("\"sqlStatementStatus\":\"").append(jsonEscape(sqlStatementStatus == null ? "" : sqlStatementStatus.trim())).append("\",")
                        .append("\"sqlStatementStartTimestamp\":\"").append(sqlStatementStartTimestamp == null ? "" : sqlStatementStartTimestamp.toString()).append("\",")
                        .append("\"databaseLockWaitTimeMs\":").append(databaseLockWaitTimeMs).append(",")
                        .append("\"nonDatabaseLockWaitTimeMs\":").append(nonDatabaseLockWaitTimeMs).append(",")
                        .append("\"lockWaitTimeMs\":").append(lockWaitTimeMs)
                        .append("},");
            }

            // Remove the last comma and close the JSON array
            if (jsonMetrics.length() > 1) {
                jsonMetrics.setLength(jsonMetrics.length() - 1);
            }
            jsonMetrics.append("]");

            response.append("{")
                    .append("\"name\":\"com.newrelic.as400-long-running-sql\",")
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

    private static String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return escaped.toString();
    }

    public static void main(String[] args) {
        String strAs400 = System.getenv("AS400HOST");
        String strUser = System.getenv("USERID");
        String strPass = System.getenv("PASSWD");
        AS400 as400 = new AS400(strAs400, strUser, strPass);

        GetLongRunningSqlInfo longRunningSqlInfo = new GetLongRunningSqlInfo();
        Map<String, String> arguments = new HashMap<>();
        arguments.put("-U", strUser);
        arguments.put("-P", strPass);
        arguments.put("-SSL", "false");

        StringBuffer response = new StringBuffer();
        longRunningSqlInfo.execute(as400, arguments, response);

        System.out.println(response.toString());
    }
}
