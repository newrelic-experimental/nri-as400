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

public class GetOutputQueueInfo {
    String Version = "\"integration_version\":\"" + CommonUtil.getIntegrationVersion() + "\",";

    public GetOutputQueueInfo() {
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
            rs = stmt.executeQuery("SELECT OUTPUT_QUEUE_NAME, OUTPUT_QUEUE_LIBRARY_NAME, OUTPUT_QUEUE_STATUS, " +
                    "NUMBER_OF_FILES FROM QSYS2.OUTPUT_QUEUE_INFO");
            if (rs == null) {
                response.append(Constants.retrieveDataError + " - " + "Cannot retrieve data from server");
                return returnValue;
            }

            StringBuilder jsonMetrics = new StringBuilder();
            jsonMetrics.append("[");

            while (rs.next()) {
                String outputQueueName = rs.getString("OUTPUT_QUEUE_NAME");
                String outputQueueLibrary = rs.getString("OUTPUT_QUEUE_LIBRARY_NAME");
                String outputQueueStatus = rs.getString("OUTPUT_QUEUE_STATUS");
                long numberOfFiles = rs.getLong("NUMBER_OF_FILES");

                jsonMetrics.append("{")
                        .append("\"event_type\":\"AS400:OutputQueueEvent\",")
                        .append("\"systemName\":\"").append(systemName).append("\",")
                        .append("\"hostName\":\"").append(CommonUtil.getHostName(as400)).append("\",")
                        .append("\"includeInIseriesEntity\":true,")
                        .append("\"outputQueueName\":\"").append(outputQueueName == null ? "" : outputQueueName.trim()).append("\",")
                        .append("\"outputQueueLibrary\":\"").append(outputQueueLibrary == null ? "" : outputQueueLibrary.trim()).append("\",")
                        .append("\"outputQueueStatus\":\"").append(outputQueueStatus == null ? "" : outputQueueStatus.trim()).append("\",")
                        .append("\"numberOfFiles\":").append(numberOfFiles)
                        .append("},");
            }

            // Remove the last comma and close the JSON array
            if (jsonMetrics.length() > 1) {
                jsonMetrics.setLength(jsonMetrics.length() - 1);
            }
            jsonMetrics.append("]");

            response.append("{")
                    .append("\"name\":\"com.newrelic.as400-output-queue-info\",")
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

        GetOutputQueueInfo outputQueueInfo = new GetOutputQueueInfo();
        Map<String, String> arguments = new HashMap<>();
        arguments.put("-U", strUser);
        arguments.put("-P", strPass);
        arguments.put("-SSL", "false");

        StringBuffer response = new StringBuffer();
        outputQueueInfo.execute(as400, arguments, response);

        System.out.println(response.toString());
    }
}
