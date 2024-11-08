package com.newrelic.as400;

import com.ibm.as400.access.AS400;
import com.newrelic.labs.utils.JDBCConnection;
import com.newrelic.labs.utils.Constants;
import com.newrelic.labs.utils.CommonUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GetDiskUsage {
    DecimalFormat usageFormat = new DecimalFormat("0.00%");
    String Version= "\"integration_version\":\"0.2.0\",";

    public GetDiskUsage() {
    }

    public int execute(AS400 as400, Map<String, String> args, StringBuffer response) {
        double percentUsed = 0.;
        String aspNumber = null;
        String unitNumber = null;
        String unitType = null;
        String diskType = null;
        String diskModel = null;
        String serialNumber = null;
        String resourceName = null;
        String resourceStatus = null;
        long capacity;
        long available;
        double maxDskUsgVal = 0;
        Statement stmt = null;
        ResultSet rs = null;
        String warningCap = args.get("-W");
        String criticalCap = args.get("-C");
        double doubleWarningCap = (warningCap == null) ? 100 : Double.parseDouble(warningCap);
        double doubleCriticalCap = (criticalCap == null) ? 100 : Double.parseDouble(criticalCap);
        int returnValue = Constants.UNKNOWN;

        Set<String> uniqueUnits = new HashSet<>();

        Connection connection = null;
        try {
            JDBCConnection JDBCConn = new JDBCConnection();
            connection = JDBCConn.getJDBCConnection(as400.getSystemName(), args.get("-U"), args.get("-P"), args.get("-SSL"));
            if (connection == null) {
                response.append(Constants.retrieveDataError + " - " + "Cannot get the JDBC connection");
                return returnValue;
            }
            stmt = connection.createStatement();
            rs = stmt.executeQuery("SELECT ASP_NUMBER, UNIT_NUMBER, UNIT_TYPE, UNIT_STORAGE_CAPACITY, UNIT_SPACE_AVAILABLE, PERCENT_USED, DISK_TYPE, DISK_MODEL, SERIAL_NUMBER, RESOURCE_NAME, RESOURCE_STATUS FROM QSYS2.SYSDISKSTAT");
            if (rs == null) {
                response.append(Constants.retrieveDataError + " - " + "Cannot retrieve data from server");
                return returnValue;
            }
            int count = 0;
            StringBuilder jsonMetrics = new StringBuilder();
            jsonMetrics.append("[");

            while (rs.next()) {
                percentUsed = rs.getDouble("PERCENT_USED");
                aspNumber = rs.getString("ASP_NUMBER");
                unitNumber = rs.getString("UNIT_NUMBER");
                unitType = rs.getString("UNIT_TYPE");
                diskType = rs.getString("DISK_TYPE");
                diskModel = rs.getString("DISK_MODEL");
                serialNumber = rs.getString("SERIAL_NUMBER");
                resourceName = rs.getString("RESOURCE_NAME");
                resourceStatus = rs.getString("RESOURCE_STATUS");
                capacity = rs.getLong("UNIT_STORAGE_CAPACITY") / 1000000;
                available = rs.getLong("UNIT_SPACE_AVAILABLE") / 1000000;

                String uniqueKey = aspNumber + "-" + unitNumber + "-" + unitType;
                if (!uniqueUnits.contains(uniqueKey)) {
                    uniqueUnits.add(uniqueKey);

                    maxDskUsgVal = percentUsed > maxDskUsgVal ? percentUsed : maxDskUsgVal;
                    returnValue = CommonUtil.getStatus(percentUsed, doubleWarningCap, doubleCriticalCap, returnValue);

                    jsonMetrics.append("{")
                            .append("\"event_type\":\"AS400:DiskUsageEvent\",")
                            .append("\"aspNumber\":\"").append(aspNumber).append("\",")
                            .append("\"unitNumber\":\"").append(unitNumber).append("\",")
                            .append("\"unitType\":\"").append(unitType).append("\",")
                            .append("\"diskType\":\"").append(diskType).append("\",")
                            .append("\"diskModel\":\"").append(diskModel).append("\",")
                            .append("\"serialNumber\":\"").append(serialNumber).append("\",")
                            .append("\"resourceName\":\"").append(resourceName).append("\",")
                            .append("\"resourceStatus\":\"").append(resourceStatus).append("\",")
                            .append("\"capacityMB\":").append(capacity).append(",")
                            .append("\"availableMB\":").append(available).append(",")
                            .append("\"percentUsed\":").append(percentUsed)
                            .append("},");
                    count++;
                }
            }

            // Remove the last comma and close the JSON array
            if (jsonMetrics.length() > 1) {
                jsonMetrics.setLength(jsonMetrics.length() - 1);
            }
            jsonMetrics.append("]");

            response.append("{")
                    .append("\"name\":\"com.newrelic.as400-disk-usage\",")
                    .append("\"protocol_version\":\"1\",")
                    .append(Version)
                    .append("\"metrics\":").append(jsonMetrics.toString()).append(",")
                    .append("\"inventory\":{},")
                    .append("\"events\":[]")
                    .append("}");

           // System.out.println("Count: " + count);
            return returnValue;
        } catch (Exception e) {
            response.setLength(0);
            response.append(Constants.retrieveDataException + " - " + e.toString());
            CommonUtil.printStack(e.getStackTrace(), response);
            CommonUtil.logError(args.get("-H"), this.getClass().getName(), e.getMessage());
            e.printStackTrace();
        } finally {
            usageFormat = null;
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

        GetDiskUsage diskUsage = new GetDiskUsage();
        Map<String, String> arguments = new HashMap<>();
        arguments.put("-U", strUser);
        arguments.put("-P", strPass);
        arguments.put("-SSL", "false"); // Adjust as needed
        arguments.put("-W", "80"); // Warning threshold
        arguments.put("-C", "90"); // Critical threshold

        StringBuffer response = new StringBuffer();
        int result = diskUsage.execute(as400, arguments, response);

        // Print the response
        System.out.println(response.toString());

        // Print the result status
        switch (result) {
            case Constants.OK:
              //  System.out.println("Status: OK");
                break;
            case Constants.WARNING:
               // System.out.println("Status: WARNING");
                break;
            case Constants.CRITICAL:
              //  System.out.println("Status: CRITICAL");
                break;
            case Constants.UNKNOWN:
            default:
              //  System.out.println("Status: UNKNOWN");
                break;
        }
    }
}