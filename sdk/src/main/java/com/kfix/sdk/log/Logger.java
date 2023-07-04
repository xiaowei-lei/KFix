package com.kfix.sdk.log;

public class Logger {
    private static final String TAG = "kfix";

    public static void i(String message) {
        System.out.println("[" + TAG + "]" + message);
    }

    public static void e(String message, Throwable throwable) {
        System.err.println("[" + TAG + "]" + message);
        throwable.printStackTrace();
    }
}
