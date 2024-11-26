package com.obfuscation.utils;

import java.util.List;
import java.util.stream.Collectors;

public class ConsoleUtils {
    public enum COLOR {
        RED,
        GREEN,
        OLIVE,
        CYAN,
        PURPLE,
        BLUE,
        GRAY
    }

    public static String intoColoredString(COLOR color, String content) {
        String colorString = null;
        switch (color) {
            case RED    : colorString = "\u001b[31m%s\u001b[0m"; break;
            case GREEN  : colorString = "\u001b[32m%s\u001b[0m"; break;
            case OLIVE  : colorString = "\u001b[33m%s\u001b[0m"; break;
            case BLUE   : colorString = "\u001b[34m%s\u001b[0m"; break;
            case PURPLE : colorString = "\u001b[35m%s\u001b[0m"; break;
            case CYAN   : colorString = "\u001b[36m%s\u001b[0m"; break;
            case GRAY   : colorString = "\u001b[37m%s\u001b[0m"; break;
        }
        return String.format(colorString, content);
    }

    public static String formatBox(String title, boolean center, List<String> lines) {
        int width = 10;
        if (title != null) {
            width = title.length() + 4;
        }
        List<String> linesOrganized = lines.stream()
                .map(line -> line.replace("\t", "    "))
                .collect(Collectors.toList());
        for (String line : linesOrganized) {
            int lineWidth = line.length() + 2;
            if (lineWidth > width) width = lineWidth;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("+");
        if (title == null) {
            addTimes(sb, width, "-");
        } else {
            centerString(sb, "[ " + title + " ]", "-", width);
        }
        sb.append("+");
        sb.append("\n");

        for (String line : linesOrganized) {
            sb.append("|");

            if (center) {
                centerString(sb, line, " ", width);
            } else {
                sb.append(" ").append(line);
                addTimes(sb, width - line.length() - 1, " ");
            }

            sb.append("|");
            sb.append("\n");
        }

        sb.append("+");
        addTimes(sb, width, "-");
        sb.append("+");

        return sb.toString();
    }

    private static void centerString(StringBuilder stringBuilder, String stringToCenter, String fillChar, int width) {
        int sideOffset = width - stringToCenter.length();

        addTimes(stringBuilder, sideOffset / 2, fillChar);

        stringBuilder.append(stringToCenter);

        addTimes(stringBuilder, sideOffset - sideOffset / 2, fillChar);
    }

    private static void addTimes(StringBuilder sb, int times, String s) {
        for (int index = 0; index < Math.max(0, times); index++) {
            sb.append(s);
        }
    }
}
