package com.example.data.api

import com.example.data.model.User
import com.example.data.model.UserOrganization

// Auth Requests
data class GoogleAuthRequest(
    val idToken: String
)

data class RegisterEmailRequest(
    val name: String,
    val orgName: String,
    val email: String,
    val role: String
)

data class VerifyEmailOtpRequest(
    val email: String,
    val otp: String
)

data class LoginEmailRequest(
    val email: String
)

data class LoginEmailVerifyRequest(
    val email: String,
    val otp: String
)

data class RegisterWhatsappRequest(
    val name: String,
    val orgName: String,
    val phone: String,
    val role: String
)

data class VerifyWhatsappOtpRequest(
    val phone: String,
    val otp: String
)

data class LoginWhatsappRequest(
    val phone: String
)

data class LoginWhatsappVerifyRequest(
    val phone: String,
    val otp: String
)

// Auth Responses
data class AuthResponse(
    val token: String?,
    val user: User?,
    val success: Boolean? = true,
    val message: String? = null
)

data class SuccessResponse(
    val success: Boolean,
    val message: String?
)

data class SessionInfo(
    val id: String,
    val user_agent: String?,
    val ip_address: String?,
    val last_active_at: String?,
    val is_current: Boolean? = false
)

// Org Request Payloads
data class CreateOrgRequest(
    val name: String,
    val description: String?,
    val type: String? = "Festival"
)

// Summary/Statistics Payloads are mapped directly from SummaryTotals
