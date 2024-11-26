package com.obfuscation.proconfig.utils;

public class ArrayUtil {

    public static boolean equalOrNull(Object[] array1, Object[] array2) {
        return array1 == null ? array2 == null : equalOrNull(array1, array2, array1.length);
    }

    public static boolean equalOrNull(Object[] array1, Object[] array2, int size) {
        return array1 == null ? array2 == null : array2 != null && equal(array1, array2, size);
    }

    public static boolean equal(Object[] array1, Object[] array2, int size) {
        for(int index = 0; index < size; ++index) {
            if (!array1[index].equals(array2[index])) {
                return false;
            }
        }

        return true;
    }

    public static int hashCodeOrNull(Object[] array) {
        return array == null ? 0 : hashCode(array, array.length);
    }

    public static int hashCodeOrNull(Object[] array, int size) {
        return array == null ? 0 : hashCode(array, size);
    }

    public static int hashCode(float[] array, int size) {
        int hashCode = 0;

        for(int index = 0; index < size; ++index) {
            hashCode = hashCode << 1 | hashCode >>> 31;
            hashCode ^= Float.floatToRawIntBits(array[index]);
        }

        return hashCode;
    }

    public static int hashCode(double[] array, int size) {
        int hashCode = 0;

        for(int index = 0; index < size; ++index) {
            hashCode = hashCode << 1 | hashCode >>> 31;
            long longBits = Double.doubleToRawLongBits(array[index]);
            hashCode = (int)((long)hashCode ^ (longBits | longBits >>> 32));
        }

        return hashCode;
    }

    public static int hashCode(Object[] array, int size) {
        int hashCode = 0;

        for(int index = 0; index < size; ++index) {
            hashCode = hashCode << 1 | hashCode >>> 31;
            hashCode ^= array[index].hashCode();
        }

        return hashCode;
    }
}
