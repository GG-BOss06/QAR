package com.qar.securitysystem.util;

import java.util.UUID;

public final class IdUtil {
    private IdUtil() {
    }

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

