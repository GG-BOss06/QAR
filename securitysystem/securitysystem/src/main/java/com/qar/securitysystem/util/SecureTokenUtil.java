package com.qar.securitysystem.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecureTokenUtil {
    private static final SecureRandom RNG = new SecureRandom();

    private SecureTokenUtil() {
    }

    public static String newTokenUrlSafe(int bytes) {
        byte[] buf = new byte[bytes];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}

