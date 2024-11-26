package com.obfuscation.proconfig.constants;

public class AccessConstants {
    public static final int PUBLIC = 1;
    public static final int PRIVATE = 2;
    public static final int PROTECTED = 4;
    public static final int STATIC = 8;
    public static final int FINAL = 16;
    public static final int SUPER = 32;
    public static final int SYNCHRONIZED = 32;
    public static final int VOLATILE = 64;
    public static final int TRANSIENT = 128;
    public static final int BRIDGE = 64;
    public static final int VARARGS = 128;
    public static final int NATIVE = 256;
    public static final int INTERFACE = 512;
    public static final int ABSTRACT = 1024;
    public static final int STRICT = 2048;
    public static final int SYNTHETIC = 4096;
    public static final int ANNOTATION = 8192;
    public static final int ENUM = 16384;
    public static final int MANDATED = 32768;
    public static final int MODULE = 32768;
    public static final int OPEN = 32;
    public static final int TRANSITIVE = 32;
    public static final int STATIC_PHASE = 64;
    public static final int RENAMED = 65536;
    public static final int REMOVED_METHODS = 131072;
    public static final int REMOVED_FIELDS = 262144;
    public static final int VALID_FLAGS_CLASS = 63025;
    public static final int VALID_FLAGS_FIELD = 20703;
    public static final int VALID_FLAGS_METHOD = 7679;
    public static final int VALID_FLAGS_PARAMETER = 36880;
    public static final int VALID_FLAGS_MODULE = 36896;
    public static final int VALID_FLAGS_REQUIRES = 36960;
    public static final int VALID_FLAGS_EXPORTS = 36864;
    public static final int VALID_FLAGS_OPENS = 36864;

    public AccessConstants() {
    }
}
