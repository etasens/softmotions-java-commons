package com.softmotions.commons.string;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

/**
 * String escape helper.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 * @version $Id$
 */
public final class EscapeHelper {

    private static final String[] URL_FIND = {"%28", "%29", "+", "%27", "%21", "%7E"};
    private static final String[] URL_REPL = {"(", ")", "%20", "'", "!", "~"};

    @Nonnull
    public static String encodeURLComponent(String val) {
        String ret;
        try {
            ret = StringUtils.replaceEach(URLEncoder.encode(val, "UTF-8"), URL_FIND, URL_REPL);
        } catch (IOException e) {
            ret = val;
        }
        return ret;
    }

    @Nullable
    public static String decodeURIComponent(String val) {
        if (val == null) {
            return null;
        }
        String result;
        try {
            result = URLDecoder.decode(val, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            result = val;
        }
        return result;
    }

    @Nonnull
    public static String toUnicodeEscape(Object val) {
        String src = String.valueOf(val);
        int length = src.length();
        if (length == 0) return src;
        StringBuilder sb = new StringBuilder(6 * length);
        for (int i = 0; i < length; ++i) {
            sb.append("\\u");
            toUnsignedString((int) src.charAt(i), 4, sb);
        }
        return sb.toString();
    }

    private static void toUnsignedString(int i, int shift, StringBuilder sb) {
        char[] buf = new char[32];
        int charPos = 32;
        int radix = 1 << shift;
        int mask = radix - 1;
        do {
            buf[--charPos] = digits[i & mask];
            i >>>= shift;
        } while (i != 0);

        int len = (32 - charPos);
        if (len < 4) {
            for (int j = 0; j < 4 - len; ++j) {
                sb.append(0);
            }
        }
        sb.append(buf, charPos, len);
    }

    private static final char[] digits = {
            '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f', 'g', 'h',
            'i', 'j', 'k', 'l', 'm', 'n',
            'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z'
    };

    @Nonnull
    public static String escapeVelocity(Object val) {
        String src = String.valueOf(val);
        if (src.contains(".")) return StringUtils.replace(src, ".", "_");
        return src;
    }

    private static final Map<Integer, String> XML_ENTRY_MAP;

    static {
        XML_ENTRY_MAP = new HashMap<Integer, String>(5);
        XML_ENTRY_MAP.put(34, "quot");
        XML_ENTRY_MAP.put(38, "amp");
        XML_ENTRY_MAP.put(60, "lt");
        XML_ENTRY_MAP.put(62, "gt");
        XML_ENTRY_MAP.put(39, "apos");
    }

    @Nullable
    public static String escapeXML(String str) {
        if (str == null) {
            return null;
        }
        final StringBuilder buf = new StringBuilder((int) (str.length() * 1.2));
        final int length = str.length();
        for (int i = 0; i < length; ++i) {
            final int ch = str.charAt(i);
            final String entityName = XML_ENTRY_MAP.get(ch);
            if (entityName == null) {
                buf.append((char) ch);
            } else {
                buf.append('&');
                buf.append(entityName);
                buf.append(';');
            }
        }
        return buf.toString();
    }


    /**
     * Translates the given String into ASCII code.
     *
     * @param input the input which contains native characters like umlauts etc
     * @return the input in which native characters are replaced through ASCII code
     */
    @Nullable
    public static String nativeToAscii(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder buffer = new StringBuilder(input.length() + 60);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c <= 0x7E) {
                buffer.append(c);
            } else {
                buffer.append("\\u");
                String hex = Integer.toHexString(c);
                for (int j = hex.length(); j < 4; j++) {
                    buffer.append('0');
                }
                buffer.append(hex);
            }
        }
        return buffer.toString();
    }


    /**
     * Translates the given String into ASCII code.
     *
     * @param input the input which contains native characters like umlauts etc
     * @return the input in which native characters are replaced through ASCII code
     */
    @Nullable
    public static String asciiToNative(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder buffer = new StringBuilder(input.length());
        boolean precedingBackslash = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (precedingBackslash) {
                switch (c) {
                    case 'f':
                        c = '\f';
                        break;
                    case 'n':
                        c = '\n';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    case 'u':
                        String hex = input.substring(i + 1, i + 5);
                        c = (char) Integer.parseInt(hex, 16);
                        i += 4;
                }
                precedingBackslash = false;
            } else {
                precedingBackslash = (c == '\\');
            }
            if (!precedingBackslash) {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }

    @Nonnull
    public static String escapeJSON(String input) {
        if (input == null) {
            return "null";
        }
        StringWriter sw = (!input.isEmpty()) ? new StringWriter(input.length()) : new StringWriter();
        int length = input.length();
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"':
                    sw.write("\\\"");
                    break;
                case '\\':
                    sw.write("\\\\");
                    break;
                case '\b':
                    sw.write("\\b");
                    break;
                case '\f':
                    sw.write("\\f");
                    break;
                case '\n':
                    sw.write("\\n");
                    break;
                case '\r':
                    sw.write("\\r");
                    break;
                case '\t':
                    sw.write("\\t");
                    break;
                case '/':
                    sw.write("\\/");
                    break;
                default:
                    //Reference: http://www.unicode.org/versions/Unicode5.1.0/
                    if ((c >= '\u0000' && c <= '\u001F') || (c >= '\u007F' && c <= '\u009F') || (c >= '\u2000' && c <= '\u20FF')) {
                        String ss = Integer.toHexString(c);
                        sw.write("\\u");
                        for (int k = 0; k < 4 - ss.length(); k++) {
                            sw.write('0');
                        }
                        sw.write(ss.toUpperCase());
                    } else {
                        sw.write(c);
                    }
            }
        }

        return sw.toString();
    }

    /**
     * Utility for encoding and decoding values according to RFC 5987. Assumes the
     * caller already knows the encoding scheme for the value.
     */
    private static final Pattern RFC5987_ENCODED_VALUE_PATTERN = Pattern.compile("%[0-9a-f]{2}|\\S",
                                                                                 Pattern.CASE_INSENSITIVE);

    public static String encodeRFC5987(final String s) throws UnsupportedEncodingException {
        return encodeRFC5987(s, "UTF-8");
    }

    // http://stackoverflow.com/questions/11302361/ (continued next line)
    // handling-filename-parameters-with-spaces-via-rfc-5987-results-in-in-filenam
    @Nonnull
    public static String encodeRFC5987(final String s, String encoding) throws UnsupportedEncodingException {
        final byte[] rawBytes = s.getBytes(encoding);
        final int len = rawBytes.length;
        final StringBuilder sb = new StringBuilder(len << 1);
        final char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                               'a', 'b', 'c', 'd', 'e', 'f'};
        final byte[] attributeChars = {'!', '#', '$', '&', '+', '-', '.', '0',
                                       '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
                                       'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q',
                                       'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '^', '_', '`', 'a',
                                       'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
                                       'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '|',
                                       '~'};
        for (final byte b : rawBytes) {
            if (Arrays.binarySearch(attributeChars, b) >= 0) {
                sb.append((char) b);
            } else {
                sb.append('%');
                sb.append(digits[0x0f & (b >>> 4)]);
                sb.append(digits[b & 0x0f]);
            }
        }
        return sb.toString();
    }

    @Nonnull
    public static String decodeRFC5987(String s, String encoding)
            throws UnsupportedEncodingException {
        Matcher matcher = RFC5987_ENCODED_VALUE_PATTERN.matcher(s);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (matcher.find()) {
            String matched = matcher.group();
            if (matched.startsWith("%")) {
                Integer value = Integer.parseInt(matched.substring(1), 16);
                bos.write(value);
            } else {
                bos.write(matched.charAt(0));
            }
        }

        return new String(bos.toByteArray(), encoding);
    }

    private EscapeHelper() {
    }
}
