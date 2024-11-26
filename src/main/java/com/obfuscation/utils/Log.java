package com.obfuscation.utils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

public class Log {

    public static AtomicBoolean suppressInfo = new AtomicBoolean(false);
    public static AtomicBoolean suppressWarn = new AtomicBoolean(true);

    public static void warn(String msg) {
        if (suppressWarn.get())
            return;
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println(time + " [WARN] : " + msg);
    }

    public static void important(ConsoleUtils.COLOR color, String msg) {
        important(ConsoleUtils.intoColoredString(color, msg));
    }

    public static void important(String msg) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println(time + " [IMPORTANT] : " + msg);
    }

    public static void info(String msg) {
        if (suppressInfo.get())
            return;
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println(time + " [INFO] : " + msg);
    }

    public static void error(String msg, Throwable e) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println(time + " [ERROR] : " + msg);
    }

    public static void error(String msg) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println(time + " [ERROR] : " + ConsoleUtils.intoColoredString(ConsoleUtils.COLOR.RED, msg));
    }

    public static void progress(String msg) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.print(msg + "\r");
    }
}