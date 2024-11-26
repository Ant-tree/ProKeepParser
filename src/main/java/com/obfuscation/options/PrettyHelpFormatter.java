package com.obfuscation.options;

import com.obfuscation.ProKeepParser;
import com.obfuscation.utils.ConsoleUtils;
import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrettyHelpFormatter implements HelpFormatter {

    private static final String INDENT = "        ";

    @Override
    public String format(Map<String, ? extends OptionDescriptor> map) {
        String usage = ConsoleUtils.formatBox("Usage", false, Arrays.asList(
                "java -jar ProKeepParser.jar \\",
                "--apk       INPUT.apk \\",
                "--config    proguard-rule.pro \\",
                "--out       OUTPUT_DIR/"
        ));
        String header = ProKeepParser.MARK + "\n" + usage + "\n\nOptions: (* for required) \n";
        String footer = "\n";

        List<String> required = new ArrayList<>();

        for (OptionDescriptor option : map.values()
                .stream()
                .filter(OptionDescriptor::isRequired)
                .collect(Collectors.toList())
        ) {
            beautifyRequired(option, required);
        }

        return header + String.join("\n", required) + "\n" + footer + "\n";
    }

    private static void beautifyRequired(OptionDescriptor option, List<String> required) {
        String optionDetails = "* " + option.options().stream()
                .map(o -> "--" + o)
                .collect(Collectors.joining(", "));
        required.add(optionDetails);

        if (!option.defaultValues().isEmpty()) {
            required.add(INDENT + "(default: " + option.defaultValues() + ")");
        }
        Arrays.asList(option.argumentDescription().split("\n")).forEach(
                line -> required.add(INDENT + line)
        );
    }

}
