package co.casterlabs.kaimen.util;

import java.security.SecureRandom;

public class Crypto {
    private static final char[] SECURE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final char[] CHARS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static char[] generateSecureRandomKey(int size) {
        char[] result = new char[size];

        for (int i = 0; i < size; i++) {
            result[i] = SECURE_CHARS[SECURE_RANDOM.nextInt(SECURE_CHARS.length)];
        }

        return result;
    }

    public static char[] generateRandomKey(int size) {
        char[] result = new char[size];

        for (int i = 0; i < size; i++) {
            result[i] = CHARS[SECURE_RANDOM.nextInt(CHARS.length)];
        }

        return result;
    }

}
