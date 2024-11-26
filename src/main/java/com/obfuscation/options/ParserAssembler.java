package com.obfuscation.options;

import joptsimple.OptionParser;

@SuppressWarnings("TextBlockMigration")
public class ParserAssembler {

    public static void run(OptionParser parser) {
        //+------------------------------------------------------------------------------------------+
        // Required Options (Order-sensitive)
        //+------------------------------------------------------------------------------------------+
        parser.accepts("apk")
                .withRequiredArg()
                .ofType(String.class)
                .required()
                .describedAs("Input apk file that takes proguard config");

        parser.accepts("config")
                .withRequiredArg()
                .ofType(String.class)
                .required()
                .describedAs("Proguard configuration file to examine.");

        parser.accepts("out")
                .withRequiredArg()
                .ofType(String.class)
                .required()
                .describedAs("Output directory where the parsed and adapted data will be saved.");

        //+------------------------------------------------------------------------------------------+
        // Etc
        //+------------------------------------------------------------------------------------------+
        parser.accepts("help").forHelp();
        parser.accepts("version").forHelp();
    }

}
