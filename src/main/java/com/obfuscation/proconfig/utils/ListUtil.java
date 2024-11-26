package com.obfuscation.proconfig.utils;

import java.util.List;

public class ListUtil {

    public static String commaSeparatedString(List<String> list, boolean quoteStrings) {
        if (list == null) {
            return null;
        } else {
            StringBuilder buffer = new StringBuilder();

            for(int index = 0; index < list.size(); ++index) {
                if (index > 0) {
                    buffer.append(',');
                }

                String string = list.get(index);
                if (quoteStrings) {
                    string = quotedString(string);
                }

                buffer.append(string);
            }

            return buffer.toString();
        }
    }

    private static int skipWhitespace(String string, int index) {
        while(index < string.length() && Character.isWhitespace(string.charAt(index))) {
            ++index;
        }

        return index;
    }

    private static String quotedString(String string) {
        return string.length() != 0
                && string.indexOf(32) < 0
                && string.indexOf(64) < 0
                && string.indexOf(123) < 0
                && string.indexOf(125) < 0
                && string.indexOf(40) < 0
                && string.indexOf(41) < 0
                && string.indexOf(58) < 0
                && string.indexOf(59) < 0
                && string.indexOf(44) < 0
                ? string
                : "'" + string + "'";
    }

}
