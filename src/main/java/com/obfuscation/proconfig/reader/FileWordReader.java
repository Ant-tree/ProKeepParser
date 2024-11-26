package com.obfuscation.proconfig.reader;

import java.io.*;
import java.net.URL;

public class FileWordReader extends LineWordReader
{
    /**
     * Creates a new FileWordReader for the given file.
     */
    public FileWordReader(File file) throws IOException
    {
        super(new LineNumberReader(
              new BufferedReader(
              new InputStreamReader(
              new FileInputStream(file), "UTF-8"))),
              "file '" + file.getPath() + "'",
              file.getParentFile());
    }


    /**
     * Creates a new FileWordReader for the given URL.
     */
    public FileWordReader(URL url) throws IOException
    {
        super(new LineNumberReader(
              new BufferedReader(
              new InputStreamReader(url.openStream(), "UTF-8"))),
              "file '" + url.toString() + "'",
              url);
    }
}
