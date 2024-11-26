package com.obfuscation;

import com.obfuscation.options.ParserAssembler;
import com.obfuscation.options.PrettyHelpFormatter;
import com.obfuscation.utils.Log;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class ProKeepParser {
    public static final String VERSION_CODE = "1.0.0.0";
    public static final String MARK = "ProKeepParser v" + VERSION_CODE + "\n";

    public static void main(String[] args) {
        OptionParser parser = new OptionParser();
        ParserAssembler.run(parser);

        try {
            OptionSet options = parser.parse(args);

            if (options.has("help")) {
                parser.formatHelpWith(new PrettyHelpFormatter());
                parser.printHelpOn(System.out);
                return;
            } else if (options.has("version")) {
                System.out.println(MARK);
                return;
            }

            String apkFilePath = options.has("apk")
                    ? (String) options.valueOf("apk") : null;

            String configFilePath = options.has("config")
                    ? (String) options.valueOf("config") : null;

            String outputDir = options.has("out")
                    ? (String) options.valueOf("out") : null;

            ProKeepParserImpl proKeepParser = new ProKeepParserImpl(
                    apkFilePath,
                    configFilePath,
                    outputDir
            );
            System.exit(proKeepParser.process());

        } catch (Exception e) {
            Log.error(e.getMessage() + " (Tip: try --help)");
        }
    }
}