package com.alsaeeddev.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.alsaeeddev.data.model.EncryptedCallerPayload
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class DecryptedCallerData(
    val name: String,
    val number: String,
    val location: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class KeystoreCryptoManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "PrivateCallerID_Master_GCM_Key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        ensureMasterKey()
    }

    private fun ensureMasterKey() {
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            try {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()

                keyGenerator.init(spec)
                keyGenerator.generateKey()
                PrivacyLogger.i("Hardware Keystore master AES-256 key initialized successfully.")
            } catch (e: Exception) {
                PrivacyLogger.e("Error initializing hardware keystore key: ${e.message}", e)
            }
        }
    }

    private fun getSecretKey(): SecretKey {
        return (keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: throw IllegalStateException("Keystore master key is unavailable")
    }

    /**
     * Encrypts caller identity into an authenticated AES-GCM payload.
     */
    fun encryptCallerData(name: String, number: String, location: String? = null): EncryptedCallerPayload {
        return try {
            val json = JSONObject().apply {
                put("name", name)
                put("number", number)
                put("location", location ?: "")
                put("ts", System.currentTimeMillis())
            }.toString()

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(json.toByteArray(Charsets.UTF_8))

            EncryptedCallerPayload(
                iv = iv,
                cipherText = cipherText,
                authTagLength = GCM_TAG_LENGTH
            )
        } catch (e: Exception) {
            PrivacyLogger.e("Encryption failure: ${e.message}", e)
            // Fallback empty payload
            EncryptedCallerPayload(
                iv = ByteArray(12),
                cipherText = ByteArray(0),
                authTagLength = GCM_TAG_LENGTH
            )
        }
    }

    /**
     * Decrypts caller identity in RAM. Returns short-lived DecryptedCallerData.
     */
    fun decryptCallerData(payload: EncryptedCallerPayload): DecryptedCallerData? {
        return try {
            if (payload.cipherText.isEmpty()) return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(payload.authTagLength, payload.iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(payload.cipherText)
            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            val json = JSONObject(jsonString)

            DecryptedCallerData(
                name = json.optString("name", "Unknown Caller"),
                number = json.optString("number", "Restricted"),
                location = json.optString("location").takeIf { it.isNotBlank() },
                timestamp = json.optLong("ts", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            PrivacyLogger.e("Decryption failed / authentication tag mismatch: ${e.message}", e)
            null
        }
    }

    fun isKeystoreHardwareBacked(): Boolean {
        return try {
            keyStore.containsAlias(MASTER_KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }
}
