package com.geollm.utils;

import com.lambdaworks.crypto.SCrypt;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PasswordUtil {
    private static final byte[] SALT = new byte[]{12, 32, 43, 54, 65, 76, 87, 98};
    private static final int N = 1 << 15;
    private static final int R = 8;
    private static final int P = 1;
    private static final int KEY_LEN = 10;

    public static String scryptBase64(String password) {
        try {
            byte[] key = SCrypt.scrypt(password.getBytes(StandardCharsets.UTF_8), SALT, N, R, P, KEY_LEN);
            return Base64.getEncoder().encodeToString(key);
        } catch (Exception e) {
            throw new IllegalStateException("scrypt failed", e);
        }
    }
}

