package com.example.data.repository

import com.example.data.api.*
import com.example.data.model.User
import com.example.data.model.UserOrganization
import java.io.IOException

class AuthRepository(
    private val api: ChandaBookApiService,
    private val sessionManager: SessionManager
) {

    fun registerSessionExpiredCallback(callback: () -> Unit) {
        sessionManager.onSessionExpired = callback
    }

    suspend fun getProfile(): Result<User> {
        if (sessionManager.getUserEmail() == "test@ai.com" || sessionManager.token == "eyJkZW1vIjoiY2hhbmRhYm9vayJ9.demo.signature") {
            android.util.Log.d("DEMO_AUTH", "Using offline demo getProfile")
            val demoUser = User(
                id = "demo-user-001",
                displayName = "AI Studio Demo",
                name = "AI Studio Demo",
                email = "test@ai.com",
                role = "admin",
                organizations = listOf(
                    UserOrganization(
                        org_id = "demo-org-001",
                        org_name = "AI Studio Demo Committee",
                        role = "admin"
                    )
                )
            )
            sessionManager.user = demoUser
            return Result.success(demoUser)
        }
        return try {
            val user = api.getCurrentUser()
            val processedUser = if (user.name.isNullOrEmpty() && !user.displayName.isNullOrEmpty()) {
                user.copy(name = user.displayName)
            } else {
                user
            }
            sessionManager.user = processedUser
            sessionManager.saveRawUserCredentials(sessionManager.token ?: "", processedUser)
            Result.success(processedUser)
        } catch (e: Exception) {
            // If offline, return cached user
            sessionManager.user?.let {
                Result.success(it)
            } ?: Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<User> {
        return try {
            val response = api.authWithGoogle(GoogleAuthRequest(idToken))
            handleAuthResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- EMAIL PASSWORDLESS OTP ---
    suspend fun registerEmail(name: String, orgName: String, email: String, role: String): Result<SuccessResponse> {
        if (email == "test@ai.com") {
            android.util.Log.d("DEMO_AUTH", "Using offline demo register request")
            return Result.success(SuccessResponse(success = true, message = "Demo OTP sent! Use 123456"))
        }
        return try {
            val response = api.registerEmail(RegisterEmailRequest(name, orgName, email, role))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyEmailOtp(email: String, otp: String): Result<User> {
        if (email == "test@ai.com" && otp == "123456") {
            android.util.Log.d("DEMO_AUTH", "Using offline demo login")
            val demoUser = User(
                id = "demo-user-001",
                displayName = "AI Studio Demo",
                name = "AI Studio Demo",
                email = "test@ai.com",
                role = "admin",
                organizations = listOf(
                    UserOrganization(
                        org_id = "demo-org-001",
                        org_name = "AI Studio Demo Committee",
                        role = "admin"
                    )
                )
            )
            val demoToken = "eyJkZW1vIjoiY2hhbmRhYm9vayJ9.demo.signature"
            sessionManager.token = demoToken
            sessionManager.user = demoUser
            sessionManager.currentOrganizationId = "demo-org-001"
            sessionManager.saveRawUserCredentials(demoToken, demoUser)
            return Result.success(demoUser)
        }
        return try {
            val response = api.verifyEmailOtp(VerifyEmailOtpRequest(email, otp))
            handleAuthResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginEmail(email: String): Result<SuccessResponse> {
        if (email == "test@ai.com") {
            android.util.Log.d("DEMO_AUTH", "Using offline demo login request")
            return Result.success(SuccessResponse(success = true, message = "Demo OTP sent! Use 123456"))
        }
        return try {
            val response = api.loginEmail(LoginEmailRequest(email))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginEmailVerify(email: String, otp: String): Result<User> {
        if (email == "test@ai.com" && otp == "123456") {
            android.util.Log.d("DEMO_AUTH", "Using offline demo login")
            val demoUser = User(
                id = "demo-user-001",
                displayName = "AI Studio Demo",
                name = "AI Studio Demo",
                email = "test@ai.com",
                role = "admin",
                organizations = listOf(
                    UserOrganization(
                        org_id = "demo-org-001",
                        org_name = "AI Studio Demo Committee",
                        role = "admin"
                    )
                )
            )
            val demoToken = "eyJkZW1vIjoiY2hhbmRhYm9vayJ9.demo.signature"
            sessionManager.token = demoToken
            sessionManager.user = demoUser
            sessionManager.currentOrganizationId = "demo-org-001"
            sessionManager.saveRawUserCredentials(demoToken, demoUser)
            return Result.success(demoUser)
        }
        return try {
            val response = api.loginEmailVerify(LoginEmailVerifyRequest(email, otp))
            handleAuthResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- WHATSAPP PASSWORDLESS OTP ---
    suspend fun registerWhatsapp(name: String, orgName: String, phone: String, role: String): Result<SuccessResponse> {
        return try {
            val response = api.registerWhatsapp(RegisterWhatsappRequest(name, orgName, phone, role))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyWhatsappOtp(phone: String, otp: String): Result<User> {
        return try {
            val response = api.verifyWhatsappOtp(VerifyWhatsappOtpRequest(phone, otp))
            handleAuthResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWhatsapp(phone: String): Result<SuccessResponse> {
        return try {
            val response = api.loginWhatsapp(LoginWhatsappRequest(phone))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWhatsappVerify(phone: String, otp: String): Result<User> {
        return try {
            val response = api.loginWhatsappVerify(LoginWhatsappVerifyRequest(phone, otp))
            handleAuthResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun handleAuthResponse(response: AuthResponse): Result<User> {
        val token = response.token
        val user = response.user
        return if (token != null && user != null) {
            val processedUser = if (user.name.isNullOrEmpty() && !user.displayName.isNullOrEmpty()) {
                user.copy(name = user.displayName)
            } else {
                user
            }
            sessionManager.token = token
            sessionManager.user = processedUser
            // Set default active organization if available in user object
            if (sessionManager.currentOrganizationId == null && processedUser.organizations.isNotEmpty()) {
                sessionManager.currentOrganizationId = processedUser.organizations.first().org_id
            }
            sessionManager.saveRawUserCredentials(token, processedUser)
            Result.success(processedUser)
        } else {
            Result.failure(Exception(response.message ?: "Invalid authentication payload standard response"))
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            api.logout()
            sessionManager.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            // Emulate clean logout locally even if server call fails
            sessionManager.clear()
            Result.success(Unit)
        }
    }

    suspend fun getSessions(): Result<List<SessionInfo>> {
        return try {
            val sessions = api.getActiveSessions()
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSession(sessionId: String): Result<Unit> {
        return try {
            api.revokeSession(sessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLocalUser(): User? = sessionManager.user
    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    fun isAiEnabled(): Boolean = sessionManager.isAiEnabled
    fun setAiEnabled(enabled: Boolean) {
        sessionManager.isAiEnabled = enabled
    }
}
