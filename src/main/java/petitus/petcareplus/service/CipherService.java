package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import petitus.petcareplus.exceptions.CipherException;
import petitus.petcareplus.utils.KeyUtil;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class CipherService {
    @Value("${application.security.jwt.secret-key}")
    private String appSecret;

    private final AESCipherService aesCipherService;

    /**
     * Encrypt plain text with secret key.
     *
     * @param plainText String
     * @param secretKey String (256 bit)
     * @return String
     * @throws RuntimeException Encrypting exception
     */
    public String encrypt(String plainText, String secretKey) {
        try {
            String key = Arrays.toString(KeyUtil.normalizeKey(secretKey));
            return aesCipherService.encrypt(plainText, key);
        } catch (Exception e) {
            throw new CipherException(e);
        }
    }

    public String encryptForURL(String plainText) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypt(plainText, appSecret).getBytes());
    }

    /**
     * Encrypt plain text with app secret.
     *
     * @param plainText String
     * @return String
     * @throws RuntimeException Encrypting exception
     */
    public String encrypt(String plainText) {
        return encrypt(plainText, appSecret);
    }

    /**
     * Decrypt cipher text with secret key.
     *
     * @param encryptedText String
     * @param secretKey     String (256 bit)
     * @return String
     * @throws RuntimeException Decrypting exception
     */
    public String decrypt(String encryptedText, String secretKey) {
        try {
            return aesCipherService.decrypt(encryptedText, secretKey);
        } catch (Exception e) {
            throw new CipherException(e);
        }
    }

    public String decryptForURL(String encryptedText) {
        return decrypt(new String(Base64.getUrlDecoder().decode(encryptedText), StandardCharsets.UTF_8));
    }


    /**
     * Decrypt cipher text with app secret.
     *
     * @param encryptedText String
     * @return String
     * @throws RuntimeException Decrypting exception
     */
    public String decrypt(String encryptedText) {
        return decrypt(encryptedText,  Arrays.toString(KeyUtil.normalizeKey(appSecret)));
    }
}