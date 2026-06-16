package com.example.data.api

import com.example.data.model.User
import com.example.data.model.UserOrganization
import com.example.data.model.Donation
import com.example.data.model.CategoryDetail
import com.example.data.model.PaymentDetail

data class DonationsResponse(
    val success: Boolean = true,
    val data: List<Donation> = emptyList(),
    val total: Int = 0
)

data class SummaryTotalsResponse(
    val success: Boolean = true,
    val summary: SummaryTotalsInner? = null,
    val byCategory: List<CategoryDetail> = emptyList(),
    val byPayment: List<PaymentDetail> = emptyList()
)

data class SummaryTotalsInner(
    val totalDonations: Int = 0,
    val totalAmount: Double = 0.0,
    val avgAmount: Double = 0.0,
    val maxAmount: Double = 0.0
)

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
    val token: String? = null,
    val user: User? = null,
    val success: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

data class SuccessResponse(
    val success: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

data class LoginResponse(
    val success: Boolean = true,
    val message: String? = null,
    val token: String? = null,
    val user: User? = null,
    val error: String? = null
)

data class OtpRequestResponse(
    val success: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

data class SessionInfo(
    val id: String = "",
    val user_agent: String? = null,
    val ip_address: String? = null,
    val last_active_at: String? = null,
    val is_current: Boolean? = false
)

// Org Request Payloads
data class CreateOrgRequest(
    val name: String,
    val description: String?,
    val type: String? = "Festival"
)

// Summary/Statistics Payloads are mapped directly from SummaryTotals
