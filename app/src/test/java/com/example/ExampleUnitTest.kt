package com.example

import com.example.crypto.CryptoEngine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.AEADBadTagException

class ExampleUnitTest {

  @Test
  fun testEcdhKeyAgreementAndSharedSecret() {
    // Generate two independent key pairs (Alice & Bob)
    val aliceKeyPair = CryptoEngine.generateEcdhKeyPair()
    val bobKeyPair = CryptoEngine.generateEcdhKeyPair()

    // Export public keys as Base64 (as transmitted over WLAN)
    val alicePubBase64 = CryptoEngine.encodePublicKey(aliceKeyPair.public)
    val bobPubBase64 = CryptoEngine.encodePublicKey(bobKeyPair.public)

    val aliceReceivedBobPub = CryptoEngine.decodePublicKey(bobPubBase64)
    val bobReceivedAlicePub = CryptoEngine.decodePublicKey(alicePubBase64)

    // Compute derived AES secret keys
    val aliceKey = CryptoEngine.deriveSharedSecret(aliceKeyPair, aliceReceivedBobPub)
    val bobKey = CryptoEngine.deriveSharedSecret(bobKeyPair, bobReceivedAlicePub)

    // Derived 256-bit AES keys must be identical!
    assertArrayEquals(aliceKey.encoded, bobKey.encoded)
    assertEquals(32, aliceKey.encoded.size) // 256-bit key
  }

  @Test
  fun testAes256GcmEncryptionAndDecryption() {
    val keyPairA = CryptoEngine.generateEcdhKeyPair()
    val keyPairB = CryptoEngine.generateEcdhKeyPair()
    val secretKey = CryptoEngine.deriveSharedSecret(keyPairA, keyPairB.public)

    val originalText = "Top-secret file contents transferred over local WLAN with zero cloud interception!"
    val originalBytes = originalText.toByteArray(Charsets.UTF_8)

    // Encrypt
    val encryptedPayload = CryptoEngine.encryptChunk(originalBytes, 0, originalBytes.size, secretKey)
    assertTrue(encryptedPayload.size > originalBytes.size) // Includes 12 bytes IV + 16 bytes GCM tag

    // Decrypt
    val decryptedBytes = CryptoEngine.decryptChunk(encryptedPayload, secretKey)
    val decryptedText = String(decryptedBytes, Charsets.UTF_8)

    assertEquals(originalText, decryptedText)
  }

  @Test(expected = Exception::class)
  fun testAesGcmTamperDetection() {
    val keyPairA = CryptoEngine.generateEcdhKeyPair()
    val keyPairB = CryptoEngine.generateEcdhKeyPair()
    val secretKey = CryptoEngine.deriveSharedSecret(keyPairA, keyPairB.public)

    val originalBytes = "Data to protect".toByteArray(Charsets.UTF_8)
    val encrypted = CryptoEngine.encryptChunk(originalBytes, 0, originalBytes.size, secretKey)

    // Tamper with a single byte in ciphertext
    encrypted[15] = (encrypted[15].toInt() xor 0xFF).toByte()

    // Decryption MUST throw an exception (AEADBadTagException) due to authentication failure
    CryptoEngine.decryptChunk(encrypted, secretKey)
  }

  @Test
  fun testSecurityFingerprintAndSasCode() {
    val keyPairA = CryptoEngine.generateEcdhKeyPair()
    val keyPairB = CryptoEngine.generateEcdhKeyPair()
    val bPubBase64 = CryptoEngine.encodePublicKey(keyPairB.public)

    val secretKey = CryptoEngine.deriveSharedSecret(keyPairA, keyPairB.public)
    val fingerprint = CryptoEngine.computeSecurityFingerprint(secretKey, bPubBase64)

    assertNotNull(fingerprint.numericCode)
    assertEquals(7, fingerprint.numericCode.length) // "123 456"
    assertNotNull(fingerprint.emojiSequence)
    assertNotNull(fingerprint.hexFingerprint)
  }

  @Test
  fun testHardwareBenchmark() {
    val benchmark = CryptoEngine.runBenchmark()
    assertTrue(benchmark.throughputMbps > 0)
    assertTrue(benchmark.cipherVerified)
  }
}
