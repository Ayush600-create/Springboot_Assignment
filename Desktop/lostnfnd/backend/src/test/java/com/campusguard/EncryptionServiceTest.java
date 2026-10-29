package com.campusguard;

import com.campusguard.service.EncryptionService;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncryptionServiceTest {

    @Test
    void thirtyTwoByteUtf8KeyIsAcceptedEvenWhenItLooksLikeBase64() {
        SecretKey key = EncryptionService.decodeKey("0123456789abcdef0123456789abcdef");
        assertEquals(32, key.getEncoded().length);
        assertEquals("AES", key.getAlgorithm());
    }

    @Test
    void rejectsWrongLengthKeys() {
        assertThrows(IllegalStateException.class, () -> EncryptionService.decodeKey("too-short"));
        assertThrows(IllegalStateException.class, () -> EncryptionService.decodeKey(""));
    }
}
