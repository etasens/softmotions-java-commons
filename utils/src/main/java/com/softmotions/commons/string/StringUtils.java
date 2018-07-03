package com.softmotions.commons.string;

import java.io.UnsupportedEncodingException;

public class StringUtils {

    static final byte[] HEX_CHAR_TABLE = {(byte) '0', (byte) '1', (byte) '2',
                                          (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',
                                          (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c',
                                          (byte) 'd', (byte) 'e', (byte) 'f'};

    private StringUtils() {
    }

    /**
     * Convert a byte array into a hex string representation
     *
     * @param raw the byte [] to convert to a hex string representation
     * @return the hex representation
     * @throws UnsupportedEncodingException
     */
    public static String getHexString(byte[] raw)
            throws UnsupportedEncodingException {
        if (raw == null) {
            return "";
        }
        byte[] hex = new byte[2 * raw.length];
        int index = 0;

        for (int i = 0; i < raw.length; i++) {
            byte b = raw[i];
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }
        return new String(hex, "ASCII");
    }
}
