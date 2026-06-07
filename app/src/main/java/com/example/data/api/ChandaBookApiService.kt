package com.example.data.api

import com.example.data.model.*
import retrofit2.http.*

interface ChandaBookApiService {

    // --- AUTHENTICATION ---
    @POST("auth/google")
    suspend fun authWithGoogle(@Body body: GoogleAuthRequest): AuthResponse

    @POST("auth/register/email")
    suspend fun registerEmail(@Body body: RegisterEmailRequest): SuccessResponse

    @POST("auth/email/verify-otp")
    suspend fun verifyEmailOtp(@Body body: VerifyEmailOtpRequest): AuthResponse

    @POST("auth/login/email")
    suspend fun loginEmail(@Body body: LoginEmailRequest): SuccessResponse

    @POST("auth/login/email/verify")
    suspend fun loginEmailVerify(@Body body: LoginEmailVerifyRequest): AuthResponse

    @POST("auth/register/whatsapp")
    suspend fun registerWhatsapp(@Body body: RegisterWhatsappRequest): SuccessResponse

    @POST("auth/whatsapp/verify-otp")
    suspend fun verifyWhatsappOtp(@Body body: VerifyWhatsappOtpRequest): AuthResponse

    @POST("auth/login/whatsapp")
    suspend fun loginWhatsapp(@Body body: LoginWhatsappRequest): SuccessResponse

    @POST("auth/login/whatsapp/verify")
    suspend fun loginWhatsappVerify(@Body body: LoginWhatsappVerifyRequest): AuthResponse

    @GET("auth/me")
    suspend fun getCurrentUser(): User

    @POST("auth/logout")
    suspend fun logout(): SuccessResponse

    @GET("auth/sessions")
    suspend fun getActiveSessions(): List<SessionInfo>

    @DELETE("auth/sessions/{sessionId}")
    suspend fun revokeSession(@Path("sessionId") sessionId: String): SuccessResponse


    // --- ORGANIZATIONS ---
    @GET("organizations")
    suspend fun getMyOrganizations(): List<Organization>

    @POST("organizations")
    suspend fun createOrganization(@Body body: CreateOrgRequest): Organization

    @GET("organizations/{id}")
    suspend fun getOrganizationDetails(@Path("id") id: String): Organization

    @PUT("organizations/{id}")
    suspend fun updateOrganization(@Path("id") id: String, @Body body: CreateOrgRequest): Organization

    @GET("organizations/{id}/members")
    suspend fun getOrganizationMembers(@Path("id") id: String): List<Member>


    // --- DONATIONS ---
    @GET("donations")
    suspend fun getDonations(
        @Query("organizationId") organizationId: String?,
        @Query("chandaBookId") chandaBookId: String?,
        @Query("category") category: String?,
        @Query("paymentMethod") paymentMethod: String?,
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?,
        @Query("minAmount") minAmount: Double?,
        @Query("maxAmount") maxAmount: Double?,
        @Query("search") search: String?,
        @Query("page") page: Int?,
        @Query("limit") limit: Int?
    ): List<Donation>

    @POST("donations")
    suspend fun createDonation(@Body donation: Donation): Donation

    @GET("donations/{id}")
    suspend fun getDonation(@Path("id") id: String): Donation

    @PUT("donations/{id}")
    suspend fun updateDonation(@Path("id") id: String, @Body donation: Donation): Donation

    @DELETE("donations/{id}")
    suspend fun deleteDonation(@Path("id") id: String): SuccessResponse

    @GET("donations/summary/totals")
    suspend fun getSummaryTotals(
        @Query("organizationId") organizationId: String?,
        @Query("chandaBookId") chandaBookId: String?,
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?
    ): SummaryTotals


    // --- EXPENSES ---
    @GET("expenses")
    suspend fun getExpenses(
        @Query("organizationId") organizationId: String?,
        @Query("chandaBookId") chandaBookId: String?,
        @Query("category") category: String?,
        @Query("paymentMethod") paymentMethod: String?
    ): List<Expense>

    @POST("expenses")
    suspend fun createExpense(@Body expense: Expense): Expense

    @GET("expenses/{id}")
    suspend fun getExpense(@Path("id") id: String): Expense

    @PUT("expenses/{id}")
    suspend fun updateExpense(@Path("id") id: String, @Body expense: Expense): Expense

    @DELETE("expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: String): SuccessResponse


    // --- USERS ---
    @GET("users")
    suspend fun getUsersInMyOrgs(): List<Member>

    @GET("users/{id}")
    suspend fun getUserProfile(@Path("id") id: String): User

    @PUT("users/{id}")
    suspend fun updateUserProfile(@Path("id") id: String, @Body body: User): User
}
