package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.User
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class SessionManager(private val context: Context) {

    var onSessionExpired: (() -> Unit)? = null

    private val prefs = context.getSharedPreferences(
        "chandabook_session", Context.MODE_PRIVATE
    )
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val userAdapter = moshi.adapter(User::class.java)

    companion object {
        private const val KEY_JWT_TOKEN = "auth_token"
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_CURRENT_ORG_ID = "current_org_id"
    }

    fun saveSession(token: String, userId: String,
                    userName: String, userEmail: String,
                    userRole: String) {
        prefs.edit()
            .putString("auth_token", token)
            .putString("user_id", userId)
            .putString("user_name", userName)
            .putString("user_email", userEmail)
            .putString("user_role", userRole)
            .putString("app_version", "1.2")
            .apply()
        Log.d("SessionManager", "Session saved: ${token.take(20)}...")
    }

    fun getToken(): String? {
        val prefs = context.getSharedPreferences(
            "chandabook_session", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)
        return if (!token.isNullOrEmpty() 
                   && token.startsWith("ey")) token else null
    }

    @get:JvmName("getJwtToken")
    @set:JvmName("setJwtToken")
    var token: String?
        get() = getToken()
        set(value) {
            if (value != null) {
                Log.d("SessionManager", "Token Saved: Persisted successfully (length=${value.length})")
                prefs.edit().putString("auth_token", value).apply()
            } else {
                Log.d("SessionManager", "Token Saved: Cleared")
                prefs.edit().remove("auth_token").apply()
            }
        }

    var user: User?
        get() {
            val savedValue = prefs.getString(KEY_USER_DATA, null) ?: return null
            return try {
                userAdapter.fromJson(savedValue)
            } catch (e: Exception) {
                Log.e("SessionManager", "User Loaded: Failed to parse plain user json: ${e.message}")
                null
            }
        }
        set(value) {
            if (value != null) {
                val json = userAdapter.toJson(value)
                Log.d("SessionManager", "User Saved: Persisted successfully")
                prefs.edit().putString(KEY_USER_DATA, json).apply()
            } else {
                Log.d("SessionManager", "User Saved: Cleared")
                prefs.edit().remove(KEY_USER_DATA).apply()
            }
        }

    var currentOrganizationId: String?
        get() = prefs.getString(KEY_CURRENT_ORG_ID, null)
        set(value) {
            prefs.edit().putString(KEY_CURRENT_ORG_ID, value).apply()
        }

    var isAiEnabled: Boolean
        get() = prefs.getBoolean("ai_enabled", true)
        set(value) = prefs.edit().putBoolean("ai_enabled", value).apply()

    fun getUserId(): String? = prefs.getString("user_id", null)
    fun getUserName(): String? = prefs.getString("user_name", null)
    fun getUserEmail(): String? = prefs.getString("user_email", null)
    fun getUserRole(): String? = prefs.getString("user_role", null)
    fun isLoggedIn(): Boolean {
        val token = getToken() ?: return false
        return token.startsWith("ey") && (token.length > 50 || token.contains("demo") || token.contains("signature"))
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

    fun clearSession() {
        prefs.edit().clear().apply()
        Log.d("SessionManager", "Session cleared")
    }

    fun clear() {
        clearSession()
    }
}
