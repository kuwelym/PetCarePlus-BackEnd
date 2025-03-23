package petitus.petcareplus.service;

import org.springframework.stereotype.Service;
import petitus.petcareplus.utils.AESCipher;

@Service
public class AESCipherService {
    public String encrypt(String plainText, String secretKey) throws Exception {
        return AESCipher.encrypt(plainText, secretKey);
    }

    public String decrypt(String encryptedText, String secretKey) throws Exception {
        return AESCipher.decrypt(encryptedText, secretKey);
    }
}