package com.ldl.customdownload;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UIUtils {
    public static String md5(final String str, final boolean isShort, final boolean isUpper) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            final byte bytes[] = md.digest();
            int i;
            final StringBuilder buf = new StringBuilder("");
            for (byte b : bytes) {
                i = b;
                if (i < 0) i += 256;
                if (i < 16) buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            String result = buf.toString();
            if (isShort) result = result.substring(8, 24);
            if (isUpper) result = result.toUpperCase();
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return str;
    }
}
