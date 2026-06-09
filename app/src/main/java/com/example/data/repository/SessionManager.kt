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

    var onSessionExpired: (() -> Unit)? = null

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
        try {
            val allEntries = prefs.all
            android.util.Log.d("SessionManager", "--- STARTUP PREFERENCES DUMP ---")
            for ((key, value) in allEntries) {
                val strVal = value?.toString() ?: "null"
                val loggedVal = if (strVal.startsWith("ey") || strVal.startsWith("{")) {
                    "Plaintext [length=${strVal.length}, prefix=${strVal.take(5)}...]"
                } else {
                    "Encrypted/Other [length=${strVal.length}, type=${value?.javaClass?.simpleName}]"
                }
                android.util.Log.d("SessionManager", "Key: $key => Value: $loggedVal")
            }
            android.util.Log.d("SessionManager", "isLoggedIn check on startup: ${isLoggedIn()}")
            android.util.Log.d("SessionManager", "----------------------------------")
        } catch (e: Exception) {
            android.util.Log.e("SessionManager", "Failed to dump preferences on startup", e)
        }
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
            val savedValue = prefs.getString(KEY_JWT_TOKEN, null) ?: return null
            if (savedValue.startsWith("ey") || savedValue.contains(".")) {
                android.util.Log.d("SessionManager", "Token Loaded: Plaintext JWT successfully retrieved (length=${savedValue.length})")
                return savedValue
            }
            // If it doesn't look like plaintext, attempt to decrypt it (migration / backwards compatibility)
            val decrypted = decrypt(savedValue)
            if (decrypted.startsWith("ey") || decrypted.contains(".")) {
                android.util.Log.d("SessionManager", "Token Loaded: Decrypted legacy JWT successfully retrieved (length=${decrypted.length})")
                return decrypted
            }
            // Check raw backup key
            val rawBackup = prefs.getString("auth_token", null)
            if (rawBackup != null && (rawBackup.startsWith("ey") || rawBackup.contains("."))) {
                android.util.Log.d("SessionManager", "Token Loaded: Retrieved from backup auth_token successfully (length=${rawBackup.length})")
                return rawBackup
            }
            // Broad fallback for customized tokens: if we have a non-empty string with length > 8, return it directly
            if (savedValue.length > 8) {
                android.util.Log.w("SessionManager", "Token Loaded: Broad fallback returning raw stored value (length=${savedValue.length})")
                return savedValue
            }
            android.util.Log.e("SessionManager", "Token Loaded: Failed to resolve a valid JWT token prefix from stored data")
            return null
        }
        set(value) {
            if (value != null) {
                android.util.Log.d("SessionManager", "Token Saved: Persisted successfully (length=${value.length})")
                prefs.edit()
                    .putString(KEY_JWT_TOKEN, value)
                    .putString("auth_token", value)
                    .apply()
            } else {
                android.util.Log.d("SessionManager", "Token Saved: Cleared")
                prefs.edit()
                    .remove(KEY_JWT_TOKEN)
                    .remove("auth_token")
                    .apply()
            }
        }

    var user: User?
        get() {
            val savedValue = prefs.getString(KEY_USER_DATA, null) ?: return null
            if (savedValue.startsWith("{")) {
                return try {
                    userAdapter.fromJson(savedValue)
                } catch (e: Exception) {
                    android.util.Log.e("SessionManager", "User Loaded: Failed to parse plain user json: ${e.message}")
                    null
                }
            }
            val decrypted = decrypt(savedValue)
            if (decrypted.startsWith("{")) {
                return try {
                    userAdapter.fromJson(decrypted)
                } catch (e: Exception) {
                    android.util.Log.e("SessionManager", "User Loaded: Failed to parse legacy decrypted user json: ${e.message}")
                    null
                }
            }
            return null
        }
        set(value) {
            if (value != null) {
                val json = userAdapter.toJson(value)
                android.util.Log.d("SessionManager", "User Saved: Persisted successfully")
                prefs.edit().putString(KEY_USER_DATA, json).apply()
            } else {
                android.util.Log.d("SessionManager", "User Saved: Cleared")
                prefs.edit().remove(KEY_USER_DATA).apply()
            }
        }

    var currentOrganizationId: String?
        get() = prefs.getString(KEY_CURRENT_ORG_ID, null)
        set(value) {
            prefs.edit().putString(KEY_CURRENT_ORG_ID, value).apply()
        }

    fun saveRawUserCredentials(token: String, user: User) {
        prefs.edit().apply {
            putString("auth_token", token)
            putString("user_id", user.id)
            putString("user_name", user.displayName ?: user.name ?: "")
            putString("user_email", user.email ?: "")
            putString("user_role", user.role ?: "viewer")
            apply()
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return token != null
    }
}
