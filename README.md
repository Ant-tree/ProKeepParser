# ProKeepParser

> ProKeepParser is a Java-based tool designed to analyze and adapt ProGuard configuration files for Android APK files.

This tool extracts class information from APK file and compare it with ProGuard configuration to filter out the classes, methods or fields that must be excluded from obfuscation process. 

This project follows the **GPL-2.0** license since the [proguard](https://github.com/Guardsquare/proguard)’s source code has been used.

## Features

- Extracts class information from Android APK files
- Analyzes and adapts ProGuard configuration files based on the extracted class information
- Outputs the classes, methods or fields to be filtered during proguard obfuscation.

### What are these for?
To check or verify the classes that should be obfuscated (or should be filtered).

Proguard config itself defines the classes or others that should be "kept", such as:
```
-keepclassmembers class * {
 public static <fields>;
 public *;
}
```
However, it is difficult to actually verify from the result that,
> Why this class should (not) be obfuscated? 

or

> How many classes should be filtered out, etc.

Thus, we figured out the way to actually prove the obfuscated classes, methods or fields by matching those with simulated result according to it's provided apk and the proguard rule file. 

## How It Works

<b><u>APK Analysis</u></b>: The tool reads the specified APK file and extracts class information from all `classes*.dex` files within the APK.
<b><u>ProGuard Configuration Adaptation</u></b>: Using the extracted class information, the tool analyzes and matches to provide the information when proguard rule actually applied.<br>This will be done by parsing `-keep` related configurations listed below.
```
-if
-keep
-keepclassmembers
-keepclasseswithmembers
-keepnames
-keepclassmembernames
-keepclasseswithmembernames
-keepcode
-dontobfuscate
allowobfuscation
includedescriptorclasses
includecode
```
<b><u>Output Generation</u></b>: The adapted ProGuard configuration is written to a new file in the specified output directory.

As an output you'll get:

```
[CLASS-NAMES]
com/sample/app/channel/identifier/Constants
com/sample/app/model/Range
com/sample/app/model/Consistency
[KEEP-METHOD-ACCESS]
com/sample/app/support/network/ConnectionManager -> 1
* -> 9
[KEEP-FIELD-ACCESS]
com/sample/app/channel/identifier/ClientBuilder$Info -> 1
* -> 1,9
[KEEP-METHOD-SIGNATURE]
com/sample/app/channel/Channel.forTarget(Ljava/lang/String;)I
com/sample/app/channel/Channel.forTargets([Ljava/lang/String;)I
[KEEP-FIELD-SIGNATURE]
com/sample/app/constans.ResultCode
```

Each section represents as follows

| Section |   | Description |
|-------------------------|:--|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [CLASS-NAMES] | | Class names that will be filtered as is. |
| [KEEP-METHOD-ACCESS] | | Access flags of a method that should be keep. This will maintain the scope of the classes that those methods locates.<br>Format: `{CLASS_SCOPE} -> {ACCESS}` |
| [KEEP-FIELD-ACCESS] | | Access flags of a field that should be keep. This will maintain the scope of the classes that those fields locates.<br>Format: `{CLASS_SCOPE} -> {ACCESS}` |
| [KEEP-METHOD-SIGNATURE] | | Keep specific method with listed signatures.<br>Signature format is:`{METHOD_OWNER_NAME}.{METHOD_NAME}{METHOD_DESC}` |
| [KEEP-FIELD-SIGNATURE] | | Keep specific field with listed signatures.<br>Signature format is:`{FIELD_OWNER_NAME}.{FIELD_NAME}`<br>This ignores the type of a field. |

For access definition, when a wildcard is provided, the access filter should be applied to every class. 

Access modifier are bitwise combination of these:

```
PUBLIC = 0x00000001
PRIVATE = 0x00000002
PROTECTED = 0x00000004
STATIC = 0x00000008
FINAL = 0x00000010
SYNCHRONIZED = 0x00000020
VOLATILE = 0x00000040
BRIDGE = 0x00000040
TRANSIENT = 0x00000080
VARARGS = 0x00000080
NATIVE = 0x00000100
INTERFACE = 0x00000200
ABSTRACT = 0x00000400
STRICT = 0x00000800
SYNTHETIC = 0x00001000
ANNOTATION = 0x00002000
ENUM = 0x00004000
MANDATED = 0x00008000
```

or refer these:
* [Proguard-core - AccessConstants](https://github.com/Guardsquare/proguard-core/blob/master/base/src/main/java/proguard/classfile/AccessConstants.java)
* [JavaDoc of Modifier fields values(Oracle docs)](https://docs.oracle.com/en/java/javase/21/docs/api/constant-values.html#java.lang.reflect.Modifier.ABSTRACT)

## Usage

To use ProKeepParser, you need to provide three key pieces of information:

1. Path to the APK file
2. Path to the ProGuard configuration file
3. Path to the output directory

Example usage:

* Use as an archive:
```bash
java -jar ProKeepParser.jar \
 --apk INPUT.apk \
 --config proguard-rule.pro \
 --out OUTPUT_DIR/ 
```
* Use as dependency:
```java
ProKeepParserImpl parser = new ProKeepParserImpl(apkFilePath, proConfigFilePath, outputDirPath);
int result = parser.process();
```
The process() method returns a result code indicating success or failure(see `com.obfuscation.constants.ResultCode`).

## Main Components
* ProKeepParserImpl: The main class that orchestrates the parsing and adaptation process.
* ProConfigAdapter: Responsible for adapting the ProGuard configuration based on the extracted class information.
* Utils: Contains utility methods, including class name normalization.

## Building and Running
This project uses Gradle as its build system. To build the project:
```
./gradlew jar
```

## Note
This tool is designed for use with Android APK files and ProGuard configuration files. 
Ensure you have the necessary permissions to analyze and modify these files before using this tool.
