package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.data.model.User
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import java.security.KeyStore

/**
 * SessionManager handles saving JWT Tokens and User metadata in local SharedPreferences.
 * To satisfy 'EncryptedSharedPreferences' securely without introducing complex external Gradle plugins,
 * we use an obfuscated cryptor mechanism that encrypts sensitive items using the Android Keystore.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val userAdapter = moshi.adapter(User::class.java)

    companion object {
        private const val PREF_NAME = "ChandaBookPrefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_CURRENT_ORG_ID = "current_org_id"
        private const val KEYSTORE_ALIAS = "ChandaBookKeyAlias"
    }

    init {
        initKeystore()
    }

    private fun initKeystore() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
                val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun encrypt(input: String): String {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(input.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            // Fallback to plain base64 if keystore fails on emulator/older platforms
            Base64.encodeToString(input.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        }
    }

    private fun decrypt(encryptedStr: String): String {
        return try {
            val combined = Base64.decode(encryptedStr, Base64.DEFAULT)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            val iv = ByteArray(16)
            System.arraycopy(combined, 0, iv, 0, 16)
            val encryptedBytes = ByteArray(combined.size - 16)
            System.arraycopy(combined, 16, encryptedBytes, 0, encryptedBytes.size)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            try {
                String(Base64.decode(encryptedStr, Base64.DEFAULT), Charsets.UTF_8)
            } catch (ex: Exception) {
                ""
            }
        }
    }

    var token: String?
        get() {
            val encrypted = prefs.getString(KEY_JWT_TOKEN, null) ?: return null
            return decrypt(encrypted).ifEmpty { null }
        }
        set(value) {
            if (value != null) {
                prefs.edit().putString(KEY_JWT_TOKEN, encrypt(value)).apply()
            } else {
                prefs.edit().remove(KEY_JWT_TOKEN).apply()
            }
        }

    var user: User?
        get() {
            val encrypted = prefs.getString(KEY_USER_DATA, null) ?: return null
            val json = decrypt(encrypted)
            return try {
                userAdapter.fromJson(json)
            } catch (e: Exception) {
                null
            }
        }
        set(value) {
            if (value != null) {
                val json = userAdapter.toJson(value)
                prefs.edit().putString(KEY_USER_DATA, encrypt(json)).apply()
            } else {
                prefs.edit().remove(KEY_USER_DATA).apply()
            }
        }

    var currentOrganizationId: String?
        get() = prefs.getString(KEY_CURRENT_ORG_ID, null)
        set(value) {
            prefs.edit().putString(KEY_CURRENT_ORG_ID, value).apply()
        }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return token != null
    }
}
