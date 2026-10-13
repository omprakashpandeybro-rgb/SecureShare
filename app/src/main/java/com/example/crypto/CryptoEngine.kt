package com.example.crypto

import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
  private const val EC_CURVE = "secp256r1"
  private const val AES_KEY_SIZE = 256
  private const val GCM_TAG_LENGTH = 128
  private const val GCM_IV_LENGTH = 12
  private const val SALT_CONTEXT = "CipherDrop-E2EE-v1-Salt"

  private val secureRandom = SecureRandom()

  val EMOJI_SECURITY_SET = arrayOf(
    "🛡️", "⚡", "💎", "🔒", "🔑", "🛰️", "🌊", "🪐",
    "🚀", "🔥", "🔮", "✨", "⚓", "🎯", "🧬", "👑"
  )

  /**
   * Generates a new ephemeral Elliptic-Curve Diffie-Hellman KeyPair on SECP256R1
   */
  fun generateEcdhKeyPair(): KeyPair {
    val kpg = KeyPairGenerator.getInstance("EC")
    kpg.initialize(ECGenParameterSpec(EC_CURVE))
    return kpg.generateKeyPair()
  }

  private fun toBase64(bytes: ByteArray): String {
    return try {
      java.util.Base64.getEncoder().encodeToString(bytes)
    } catch (_: Throwable) {
      android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }
  }

  private fun fromBase64(str: String): ByteArray {
    return try {
      java.util.Base64.getDecoder().decode(str)
    } catch (_: Throwable) {
      android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
    }
  }

  /**
   * Converts a Public Key into standard X.509 Base64 string for transmission
   */
  fun encodePublicKey(publicKey: PublicKey): String {
    return toBase64(publicKey.encoded)
  }

  /**
   * Decodes a Base64-encoded X.509 public key received from peer
   */
  fun decodePublicKey(base64Key: String): PublicKey {
    val bytes = fromBase64(base64Key)
    val spec = X509EncodedKeySpec(bytes)
    val factory = KeyFactory.getInstance("EC")
    return factory.generatePublic(spec)
  }

  /**
   * Computes ECDH shared secret and derives a 256-bit AES secret key
   */
  fun deriveSharedSecret(myKeyPair: KeyPair, peerPublicKey: PublicKey): SecretKey {
    val agreement = KeyAgreement.getInstance("ECDH")
    agreement.init(myKeyPair.private)
    agreement.doPhase(peerPublicKey, true)
    val rawSecret = agreement.generateSecret()

    // Key Derivation Function (KDF): SHA-256(rawSecret || salt)
    val md = MessageDigest.getInstance("SHA-256")
    md.update(rawSecret)
    md.update(SALT_CONTEXT.toByteArray(Charsets.UTF_8))
    val derivedKeyBytes = md.digest()

    return SecretKeySpec(derivedKeyBytes, "AES")
  }

  /**
   * Computes Short Authentication String (SAS) & Emoji Fingerprint for MITM verification
   */
  fun computeSecurityFingerprint(secretKey: SecretKey, peerPublicBase64: String): SecurityFingerprint {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(secretKey.encoded)
    md.update(peerPublicBase64.toByteArray(Charsets.UTF_8))
    val digest = md.digest()

    // 6-digit numeric verification code
    val byteBuffer = ByteBuffer.wrap(digest, 0, 4)
    val intVal = Math.abs(byteBuffer.int)
    val code = String.format("%06d", intVal % 1_000_000)
    val formattedCode = "${code.substring(0, 3)} ${code.substring(3)}"

    // 4 emoji visual checksum
    val emojiBuilder = StringBuilder()
    for (i in 0 until 4) {
      val idx = (digest[i].toInt() and 0xFF) % EMOJI_SECURITY_SET.size
      emojiBuilder.append(EMOJI_SECURITY_SET[idx])
    }

    // Hex fingerprint
    val hexBuilder = StringBuilder()
    for (i in 0 until 6) {
      hexBuilder.append(String.format("%02X", digest[i]))
      if (i < 5) hexBuilder.append(":")
    }

    return SecurityFingerprint(
      numericCode = formattedCode,
      emojiSequence = emojiBuilder.toString(),
      hexFingerprint = hexBuilder.toString()
    )
  }

  /**
   * Encrypts a chunk using AES-256-GCM.
   * Returns [12 bytes IV + ciphertext + 16 bytes GCM auth tag]
   */
  fun encryptChunk(data: ByteArray, offset: Int, length: Int, secretKey: SecretKey): ByteArray {
    val iv = ByteArray(GCM_IV_LENGTH)
    secureRandom.nextBytes(iv)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

    val ciphertext = cipher.doFinal(data, offset, length)
    val output = ByteArray(GCM_IV_LENGTH + ciphertext.size)
    System.arraycopy(iv, 0, output, 0, GCM_IV_LENGTH)
    System.arraycopy(ciphertext, 0, output, GCM_IV_LENGTH, ciphertext.size)
    return output
  }

  /**
   * Decrypts a chunk using AES-256-GCM.
   * Input format: [12 bytes IV + ciphertext with GCM tag]
   */
  fun decryptChunk(encryptedPayload: ByteArray, secretKey: SecretKey): ByteArray {
    require(encryptedPayload.size >= GCM_IV_LENGTH + 16) { "Payload too small for AES-GCM" }

    val iv = ByteArray(GCM_IV_LENGTH)
    System.arraycopy(encryptedPayload, 0, iv, 0, GCM_IV_LENGTH)

    val ciphertextLength = encryptedPayload.size - GCM_IV_LENGTH
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

    return cipher.doFinal(encryptedPayload, GCM_IV_LENGTH, ciphertextLength)
  }

  /**
   * Calculates SHA-256 hash of a byte array or stream
   */
  fun sha256Hex(bytes: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.joinToString("") { String.format("%02x", it) }
  }

  /**
   * Runs local hardware AES-256-GCM encryption benchmark
   */
  fun runBenchmark(): BenchmarkResult {
    val testSize = 4 * 1024 * 1024 // 4 MB test block
    val testData = ByteArray(testSize)
    secureRandom.nextBytes(testData)

    val keyPairA = generateEcdhKeyPair()
    val keyPairB = generateEcdhKeyPair()
    val startTime = System.nanoTime()

    val secretKey = deriveSharedSecret(keyPairA, keyPairB.public)
    val encrypted = encryptChunk(testData, 0, testSize, secretKey)
    val decrypted = decryptChunk(encrypted, secretKey)

    val durationMs = (System.nanoTime() - startTime) / 1_000_000.0
    val verified = decrypted.contentEquals(testData)
    val speedMbps = if (durationMs > 0) ((testSize / (1024.0 * 1024.0)) / (durationMs / 1000.0)) else 0.0

    return BenchmarkResult(
      throughputMbps = speedMbps,
      timeMs = durationMs,
      cipherVerified = verified,
      algorithm = "AES-256-GCM + ECDH-P256"
    )
  }
}

data class SecurityFingerprint(
  val numericCode: String,
  val emojiSequence: String,
  val hexFingerprint: String
)

data class BenchmarkResult(
  val throughputMbps: Double,
  val timeMs: Double,
  val cipherVerified: Boolean,
  val algorithm: String
)
