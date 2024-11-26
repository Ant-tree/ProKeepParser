package com.obfuscation.proconfig.reader;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.URL;

public class LineWordReader extends WordReader {
    private final LineNumberReader reader;
    private final String description;


    /**
     * Creates a new LineWordReader for the given input.
     */
    public LineWordReader(LineNumberReader lineNumberReader,
                          String description,
                          File baseDir) throws IOException {
        super(baseDir);

        this.reader = lineNumberReader;
        this.description = description;
    }


    /**
     * Creates a new LineWordReader for the given input.
     */
    public LineWordReader(LineNumberReader lineNumberReader,
                          String description,
                          URL baseURL) throws IOException {
        super(baseURL);

        this.reader = lineNumberReader;
        this.description = description;
    }


    // Implementations for WordReader.

    protected String nextLine() throws IOException {
        return reader.readLine();
    }


    public String lineLocationDescription() {
        return "line " + reader.getLineNumber() + " of " + description;
    }


    public void close() throws IOException {
        super.close();

        if (reader != null) {
            reader.close();
        }
    }
}
