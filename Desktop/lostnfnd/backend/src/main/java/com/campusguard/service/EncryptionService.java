package com.campusguard.service;

import com.campusguard.config.CampusGuardProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM for private identifying details. Hashing is not used because
 * verification needs a similarity score against the original text.
 */
@Service
public class EncryptionService {

    private static final int NONCE_LEN = 12;
    private static final int TAG_BITS = 128;
    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public EncryptionService(CampusGuardProperties properties) {
        this.key = decodeKey(properties.getCrypto().getKey());
    }

    public record Sealed(byte[] ciphertext, byte[] nonce) {}

    public Sealed encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_LEN];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new Sealed(ct, nonce);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to encrypt private details");
        }
    }

    public String decrypt(byte[] ciphertext, byte[] nonce) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to decrypt private details");
        }
    }

    public static SecretKey decodeKey(String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Encryption key is not configured");
        }
        String value = configured.trim();
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
        if (utf8.length == 32) {
            return new SecretKeySpec(utf8, "AES");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            if (decoded.length == 32) {
                return new SecretKeySpec(decoded, "AES");
            }
        } catch (IllegalArgumentException ignored) {
            // Not Base64; fall through to the length error.
        }
        throw new IllegalStateException("AES-256 key must be 32 bytes");
    }
}
