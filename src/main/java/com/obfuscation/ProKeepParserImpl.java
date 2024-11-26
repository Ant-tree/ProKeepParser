package com.obfuscation;

import com.googlecode.d2j.node.DexClassNode;
import com.googlecode.d2j.node.DexFileNode;
import com.googlecode.d2j.reader.DexFileReader;
import com.obfuscation.constants.ResultCode;
import com.obfuscation.proconfig.ProConfigAdapter;
import com.obfuscation.utils.Log;
import com.obfuscation.utils.Utils;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("CallToPrintStackTrace")
public class ProKeepParserImpl {
    private final String apkFilePath;
    private final String proConfigFilePath;
    private final String outputDirPath;

    /**
     * Constructs a new ProKeepParserImpl instance with the specified file paths.
     *
     * @param apkFilePath the file path to the APK file to be processed.
     * @param proConfigFilePath the file path to the ProGuard configuration file.
     * @param outputDirPath the directory path where the output files will be saved.
     */
    public ProKeepParserImpl(
            String apkFilePath, 
            String proConfigFilePath, 
            String outputDirPath
    ) {
        this.apkFilePath = apkFilePath;
        this.proConfigFilePath = proConfigFilePath;
        this.outputDirPath = outputDirPath;
    }

    /**
     * Processes the APK and ProGuard configuration files to adapt and output the configuration.
     * <p>
     * This method collects class nodes from the specified APK file, adapts the ProGuard configuration
     * using these class nodes, and writes the adapted configuration to the specified output directory.
     * </p>
     */
    public int process() {
        String proConfigFileName    = new File(proConfigFilePath).getName();
        String outputFilePath       = outputDirPath + File.separator + proConfigFileName;
        
        Map<String, DexClassNode> classPath = new HashMap<>();
        int result = collectClassNodes(apkFilePath, classPath);
        if (result != ResultCode.SUCCESS) {
            Log.error("Failed to collect class nodes from APK.");
            return result;
        }

        ProConfigAdapter adapter = new ProConfigAdapter(classPath);
        result = adapter.adapt(proConfigFilePath);
        if (result != ResultCode.SUCCESS) {
            return result;
        }

        result = adapter.writeAsFile(outputFilePath);
        return result;
    }

    /**
     * Collects class nodes from the specified APK file and populates the provided map with class names
     * and their corresponding DexClassNode objects.
     *
     * @param apkFilePath the file path to the APK file from which class nodes are to be collected.
     * @param classPath a map to be populated with class names as keys and DexClassNode objects as values.
     * @return an integer indicating the result of the operation, where  indicates success
     *         and {@link ResultCode#FAILED} indicates failure.
     */
    public int collectClassNodes(String apkFilePath, Map<String, DexClassNode> classPath) {
        try (ZipFile zipFile = new ZipFile(apkFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                // Check if the entry is a .dex file
                if (!entry.getName().startsWith("classes") ||
                    !entry.getName().endsWith(".dex")
                ) {
                    continue;
                }

                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    DexFileReader reader = new DexFileReader(inputStream);
                    reader.getClassNames();
                    DexFileNode preservedNode = new DexFileNode();
                    reader.accept(preservedNode);
                    preservedNode.clzs.forEach(node -> classPath.put(
                            Utils.normalizeClassName(node.className),
                            node
                    ));
                }
            }
            return ResultCode.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResultCode.FAILED;
    }
}
