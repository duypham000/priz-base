package com.priz.base.infrastructure.security.apikey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * AES-256-GCM encrypt/decrypt cho HMAC secret key của API key.
 * Dùng password-based key derivation (PBKDF2) từ cấu hình.
 */
@Component
public class SecretCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BIT = 128;
    private static final int IV_BYTE = 12;
    private static final int SALT_BYTE = 16;
    private static final int KEY_BIT = 256;
    private static final int ITERATION = 65536;

    private final char[] masterKey;

    public SecretCipher(
            @Value("${priz.security.api-key.secret-encryption-key}") String encryptionKey) {
        this.masterKey = encryptionKey.toCharArray();
    }

    public String encrypt(String plaintext) {
        try {
            byte[] salt = new byte[SALT_BYTE];
            byte[] iv = new byte[IV_BYTE];
            new SecureRandom().nextBytes(salt);
            new SecureRandom().nextBytes(iv);

            SecretKeySpec key = deriveKey(salt);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BIT, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            byte[] combined = new byte[SALT_BYTE + IV_BYTE + ciphertext.length];
            System.arraycopy(salt, 0, combined, 0, SALT_BYTE);
            System.arraycopy(iv, 0, combined, SALT_BYTE, IV_BYTE);
            System.arraycopy(ciphertext, 0, combined, SALT_BYTE + IV_BYTE, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            byte[] salt = new byte[SALT_BYTE];
            byte[] iv = new byte[IV_BYTE];
            byte[] ciphertext = new byte[combined.length - SALT_BYTE - IV_BYTE];
            System.arraycopy(combined, 0, salt, 0, SALT_BYTE);
            System.arraycopy(combined, SALT_BYTE, iv, 0, IV_BYTE);
            System.arraycopy(combined, SALT_BYTE + IV_BYTE, ciphertext, 0, ciphertext.length);

            SecretKeySpec key = deriveKey(salt);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BIT, iv));
            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    private SecretKeySpec deriveKey(byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(masterKey, salt, ITERATION, KEY_BIT);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
}
