package com.newrelic.labs.utils;

import com.ibm.as400.access.AS400;

public class CommonUtil {
    public static String getHostName(AS400 as400) {
        String override = System.getenv("HOSTNAME_OVERRIDE");
        return (override != null && !override.isEmpty()) ? override : as400.getSystemName();
    }

    /**
     * Reads the real build version from the jar's manifest (set by the shade
     * plugin to match the pom.xml version), so it can't drift out of sync
     * across releases the way a hardcoded string in each class would.
     * Falls back to "unknown" when not running from a packaged jar
     * (e.g. running directly from target/classes during development).
     */
    public static String getIntegrationVersion() {
        String version = CommonUtil.class.getPackage().getImplementationVersion();
        return (version != null && !version.isEmpty()) ? version : "unknown";
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