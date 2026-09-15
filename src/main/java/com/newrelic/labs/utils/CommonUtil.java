package com.newrelic.labs.utils;

import com.ibm.as400.access.AS400;

public class CommonUtil {
    public static String getHostName(AS400 as400) {
        String override = System.getenv("HOSTNAME_OVERRIDE");
        return (override != null && !override.isEmpty()) ? override : as400.getSystemName();
    }

    public static int getStatus(double value, double warningThreshold, double criticalThreshold, int currentStatus) {
        if (value >= criticalThreshold) {
            return Constants.CRITICAL;
        } else if (value >= warningThreshold) {
            return Constants.WARNING;
        } else {
            return Constants.OK;
        }
    }

    public static void printStack(StackTraceElement[] stackTrace, StringBuffer response) {
        for (StackTraceElement element : stackTrace) {
            response.append(element.toString()).append("\n");
        }
    }

    public static void logError(String host, String className, String message) {
        System.err.println("Error [" + host + "][" + className + "]: " + message);
    }
}