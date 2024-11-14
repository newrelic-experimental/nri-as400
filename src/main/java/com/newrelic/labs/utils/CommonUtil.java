package com.newrelic.labs.utils;

public class CommonUtil {
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