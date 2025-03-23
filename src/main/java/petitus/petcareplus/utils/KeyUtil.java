package petitus.petcareplus.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class KeyUtil {
    public static byte[] normalizeKey(String secretKey) {
        try {
            byte[] keyBytes;
            try {
                keyBytes = Base64.getDecoder().decode(secretKey);
            } catch (IllegalArgumentException e) {
                keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            }

            if (keyBytes.length >= 32) {
                return Arrays.copyOf(keyBytes, 32);
            }

            // Hash to get a 256-bit (32-byte) key if too short
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return Arrays.copyOf(sha.digest(keyBytes), 32);
        } catch (Exception e) {
            throw new RuntimeException("Error normalizing key", e);
        }
    }
}
