package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ChandaBookApplication
import com.example.data.model.*
import com.example.data.repository.*
import com.example.data.api.SessionInfo
import com.example.utils.CurrencyFormatter
import com.example.utils.PdfGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChandaBookViewModel(
    application: Application,
    private val authRepo: AuthRepository,
    private val orgRepo: OrgRepository,
    private val donationRepo: DonationRepository,
    private val expenseRepo: ExpenseRepository
) : AndroidViewModel(application) {

    // --- SYSTEM & COROUTINE JOBS ---
    private var timerJob: Job? = null

    // --- GLOBAL STATE ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()

    // --- AUTHENTICATION STATE ---
    private val _currentUser = MutableStateFlow<User?>(authRepo.getLocalUser())
    val currentUser = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(authRepo.isLoggedIn())
    val isLoggedIn = _isLoggedIn.asStateFlow()

    val otpTimer = MutableStateFlow(0) // 60s countdown
    val otpSent = MutableStateFlow(false)
    val authTarget = MutableStateFlow("") // email or phone number being validated

    private val _activeSessions = MutableStateFlow<List<SessionInfo>>(emptyList())
    val activeSessions = _activeSessions.asStateFlow()

    // --- ORGANIZATIONS STATE ---
    val organizations: StateFlow<List<Organization>> = orgRepo.organizationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeOrgId = MutableStateFlow<String?>(orgRepo.getActiveOrgId())
    val activeOrgId = _activeOrgId.asStateFlow()

    private val _activeOrgDetails = MutableStateFlow<Organization?>(null)
    val activeOrgDetails = _activeOrgDetails.asStateFlow()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members = _members.asStateFlow()

    // --- DONATIONS STATE ---
    // Observe donations reactively from database
    private val _donations = MutableStateFlow<List<Donation>>(emptyList())
    
    // Filters
    val donationSearchQuery = MutableStateFlow("")
    val donationFilterCategory = MutableStateFlow("All") // "All", "donation", "prasad", etc.
    val donationFilterPayment = MutableStateFlow("All") // "All", "cash", "upi", etc.
    val donationFilterStartDate = MutableStateFlow<String?>(null) // YYYY-MM-DD
    val donationFilterEndDate = MutableStateFlow<String?>(null)

    // Local data class to combine filters seamlessly
    private data class DonationFilters(
        val query: String,
        val category: String,
        val payment: String,
        val start: String?,
        val end: String?
    )

    private val donationFiltersFlow = combine(
        donationSearchQuery,
        donationFilterCategory,
        donationFilterPayment,
        donationFilterStartDate,
        donationFilterEndDate
    ) { search, category, payment, start, end ->
        DonationFilters(search, category, payment, start, end)
    }

    // Filtered donations flow
    val filteredDonations: StateFlow<List<Donation>> = combine(
        _donations,
        donationFiltersFlow
    ) { list, filters ->
        list.filter { item ->
            val matchesSearch = filters.query.isEmpty() ||
                    item.donorName.contains(filters.query, ignoreCase = true) ||
                    (item.phoneNumber?.contains(filters.query) ?: false) ||
                    item.receiptNumber.contains(filters.query, ignoreCase = true)
            
            val matchesCategory = filters.category == "All" || item.category.lowercase() == filters.category.lowercase()
            
            val matchesPayment = filters.payment == "All" || item.paymentMethod.lowercase() == filters.payment.lowercase()
            
            val matchesDates = (filters.start == null || item.createdAt >= filters.start) && 
                    (filters.end == null || item.createdAt <= "${filters.end} T23:59:59")
            
            matchesSearch && matchesCategory && matchesPayment && matchesDates
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- EXPENSES STATE ---
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())

    val expenseSearchQuery = MutableStateFlow("")
    val expenseFilterCategory = MutableStateFlow("All")
    val expenseFilterPayment = MutableStateFlow("All")

    val filteredExpenses: StateFlow<List<Expense>> = combine(
        _expenses,
        expenseSearchQuery,
        expenseFilterCategory,
        expenseFilterPayment
    ) { list, search, category, payment ->
        list.filter { item ->
            val matchesSearch = search.isEmpty() || item.title.contains(search, ignoreCase = true)
            val matchesCategory = category == "All" || item.category.lowercase() == category.lowercase()
            val matchesPayment = payment == "All" || item.paymentMethod.lowercase() == payment.lowercase()
            
            matchesSearch && matchesCategory && matchesPayment
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- STATS SUMMARY ---
    private val _summary = MutableStateFlow<SummaryTotals?>(null)
    val summary = _summary.asStateFlow()

    // Receipt context preview
    val selectedDonation = MutableStateFlow<Donation?>(null)
    val generatedReceiptFile = MutableStateFlow<File?>(null)

    // --- INITIALIZATION ---
    init {
        // Collect local Room cached lists automatically whenever active orgId changes
        viewModelScope.launch {
            activeOrgId.collect { orgId ->
                if (orgId != null) {
                    // Update org details
                    orgRepo.getOrgDetails(orgId).onSuccess {
                        _activeOrgDetails.value = it
                    }

                    // Collect Donations
                    donationRepo.getDonationsFlow(orgId).collect {
                        _donations.value = it
                        recomputeOfflineStats(it, _expenses.value)
                    }
                }
            }
        }

        viewModelScope.launch {
            activeOrgId.collect { orgId ->
                if (orgId != null) {
                    // Collect Expenses
                    expenseRepo.getExpensesFlow(orgId).collect {
                        _expenses.value = it
                        recomputeOfflineStats(_donations.value, it)
                    }
                }
            }
        }

        // Auto load user context
        if (isLoggedIn.value) {
            refreshUserData()
        }
    }

    private fun recomputeOfflineStats(donations: List<Donation>, expenses: List<Expense>) {
        // Recompute the totals structure locally for snappy offline support
        val totalCollection = donations.sumOf { it.amount }
        val avg = if (donations.isNotEmpty()) totalCollection / donations.size else 0.0
        val max = if (donations.isNotEmpty()) donations.maxOf { it.amount } else 0.0

        val byCat = donations.groupBy { it.category }.map { (cat, items) ->
            CategoryDetail(cat, items.size, items.sumOf { it.amount })
        }
        val byPay = donations.groupBy { it.paymentMethod }.map { (pay, items) ->
            PaymentDetail(pay, items.size, items.sumOf { it.amount })
        }

        _summary.value = SummaryTotals(
            totalDonations = donations.size,
            totalAmount = totalCollection,
            avgAmount = avg,
            maxAmount = max,
            byCategory = byCat,
            byPayment = byPay
        )
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }

    fun showSuccess(msg: String) {
        _successMessage.value = msg
    }

    fun showError(msg: String) {
        _errorMessage.value = msg
    }

    // --- REFRESH ACTIONS ---
    fun refreshUserData() {
        viewModelScope.launch {
            _isLoading.value = true
            authRepo.getProfile().onSuccess { user ->
                _currentUser.value = user
                _isLoggedIn.value = true
                
                // Fetch orgs
                orgRepo.fetchMyOrganizations().onSuccess { orgList ->
                    if (orgList.isNotEmpty() && _activeOrgId.value == null) {
                        switchOrganization(orgList.first().id)
                    }
                }
            }.onFailure {
                handleApiError(it)
            }
            _isLoading.value = false
        }
    }

    fun refreshActiveOrgData() {
        val orgId = activeOrgId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            
            // Refresh details, donations, expenses, members simultaneously
            val detailsResult = orgRepo.getOrgDetails(orgId)
            detailsResult.onSuccess { _activeOrgDetails.value = it }

            val donsResult = donationRepo.refreshDonations(orgId)
            val expsResult = expenseRepo.refreshExpenses(orgId)
            val membersResult = orgRepo.getMembers(orgId)
            
            membersResult.onSuccess { _members.value = it }
            
            // Load live analytics from API
            donationRepo.getSummaryTotals(orgId).onSuccess {
                _summary.value = it
            }

            _isLoading.value = false
        }
    }

    fun switchOrganization(orgId: String) {
        orgRepo.setActiveOrgId(orgId)
        _activeOrgId.value = orgId
        
        // Reset list filters
        donationSearchQuery.value = ""
        donationFilterCategory.value = "All"
        donationFilterPayment.value = "All"
        donationFilterStartDate.value = null
        donationFilterEndDate.value = null
        
        expenseSearchQuery.value = ""
        expenseFilterCategory.value = "All"
        expenseFilterPayment.value = "All"

        // Trigger network reload
        refreshActiveOrgData()
    }

    // --- AUTH ACTIONS ---
    fun startOtpCountdown() {
        timerJob?.cancel()
        otpTimer.value = 60
        timerJob = viewModelScope.launch {
            while (otpTimer.value > 0) {
                delay(1000)
                otpTimer.value -= 1
            }
        }
    }

    fun selectGoogleLogin(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepo.loginWithGoogle(idToken).onSuccess {
                _currentUser.value = it
                _isLoggedIn.value = true
                onSuccess()
            }.onFailure {
                showError("Google verification failed. Use OTP fallback: ${it.localizedMessage}")
            }
            _isLoading.value = false
        }
    }

    fun requestEmailOtp(email: String, isNewUser: Boolean, name: String = "", orgName: String = "", role: String = "") {
        authTarget.value = email
        viewModelScope.launch {
            _isLoading.value = true
            if (isNewUser) {
                authRepo.registerEmail(name, orgName, email, role).onSuccess {
                    otpSent.value = true
                    startOtpCountdown()
                    showSuccess(it.message ?: "OTP code generated and dispatched")
                }.onFailure {
                    showError(it.localizedMessage ?: "Registration failed")
                }
            } else {
                authRepo.loginEmail(email).onSuccess {
                    otpSent.value = true
                    startOtpCountdown()
                    showSuccess(it.message ?: "OTP code sent to $email")
                }.onFailure {
                    showError(it.localizedMessage ?: "Email not registered / API error")
                }
            }
            _isLoading.value = false
        }
    }

    fun verifyEmailOtp(otp: String, isNewUser: Boolean, onSuccess: () -> Unit) {
        val email = authTarget.value
        viewModelScope.launch {
            _isLoading.value = true
            val result = if (isNewUser) {
                authRepo.verifyEmailOtp(email, otp)
            } else {
                authRepo.loginEmailVerify(email, otp)
            }
            result.onSuccess {
                _currentUser.value = it
                _isLoggedIn.value = true
                otpSent.value = false
                refreshUserData()
                onSuccess()
            }.onFailure {
                showError("Invalid OTP. Try again.")
            }
            _isLoading.value = false
        }
    }

    fun requestWhatsappOtp(phone: String, isNewUser: Boolean, name: String = "", orgName: String = "", role: String = "") {
        authTarget.value = phone
        viewModelScope.launch {
            _isLoading.value = true
            if (isNewUser) {
                authRepo.registerWhatsapp(name, orgName, phone, role).onSuccess {
                    otpSent.value = true
                    startOtpCountdown()
                    showSuccess("OTP generated on WhatsApp")
                }.onFailure {
                    showError(it.localizedMessage ?: "WhatsApp Registration failed")
                }
            } else {
                authRepo.loginWhatsapp(phone).onSuccess {
                    otpSent.value = true
                    startOtpCountdown()
                    showSuccess("OTP text message dispatched via WhatsApp")
                }.onFailure {
                    showError(it.localizedMessage ?: "WhatsApp Login request failed")
                }
            }
            _isLoading.value = false
        }
    }

    fun verifyWhatsappOtp(otp: String, isNewUser: Boolean, onSuccess: () -> Unit) {
        val phone = authTarget.value
        viewModelScope.launch {
            _isLoading.value = true
            val result = if (isNewUser) {
                authRepo.verifyWhatsappOtp(phone, otp)
            } else {
                authRepo.loginWhatsappVerify(phone, otp)
            }
            result.onSuccess {
                _currentUser.value = it
                _isLoggedIn.value = true
                otpSent.value = false
                refreshUserData()
                onSuccess()
            }.onFailure {
                showError("Invalid OTP. Please check code.")
            }
            _isLoading.value = false
        }
    }

    fun cancelAuth() {
        otpSent.value = false
        timerJob?.cancel()
    }

    fun logoutUser(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepo.logout()
            _isLoggedIn.value = false
            _currentUser.value = null
            _activeOrgId.value = null
            _activeOrgDetails.value = null
            _donations.value = emptyList()
            _expenses.value = emptyList()
            _members.value = emptyList()
            _summary.value = null
            onSuccess()
            _isLoading.value = false
        }
    }

    // --- SESSIONS ---
    fun loadSessions() {
        viewModelScope.launch {
            authRepo.getSessions().onSuccess {
                _activeSessions.value = it
            }
        }
    }

    fun revokeSession(sessionId: String) {
        viewModelScope.launch {
            authRepo.deleteSession(sessionId).onSuccess {
                loadSessions()
                showSuccess("Session revoked successfully")
            }.onFailure {
                showError("Could not revoke session")
            }
        }
    }

    // --- ORGANIZATIONS CREATION ---
    fun createNewOrg(name: String, description: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            orgRepo.createOrganization(name, description).onSuccess {
                showSuccess("Committee '${it.name}' created!")
                switchOrganization(it.id)
                refreshUserData()
                onSuccess()
            }.onFailure {
                showError("Organization create failed: ${it.localizedMessage}")
            }
            _isLoading.value = false
        }
    }

    fun editOrgDetails(orgId: String, name: String, description: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            orgRepo.updateOrganization(orgId, name, description).onSuccess {
                _activeOrgDetails.value = it
                showSuccess("Committee updated successfully")
            }.onFailure {
                showError("Failed to update committee")
            }
            _isLoading.value = false
        }
    }

    // --- DONATIONS CRUDS ---
    fun addDonation(
        donorName: String,
        phone: String?,
        amount: Double,
        category: String,
        paymentMethod: String,
        address: String?,
        notes: String?,
        onSuccess: (Donation) -> Unit
    ) {
        val orgId = activeOrgId.value ?: return showError("Please select or create an organization first")
        viewModelScope.launch {
            _isLoading.value = true
            // Generate standard unique receipt number inside ViewModel securely
            val random = (1000..9999).random()
            val prefix = orgRepo.getActiveOrgId()?.take(3)?.uppercase(Locale.getDefault()) ?: "CB"
            val receiptNo = "RC-$prefix-$random"

            donationRepo.createDonation(
                donorName = donorName,
                phoneNumber = phone,
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                address = address,
                notes = notes,
                receiptNumber = receiptNo,
                orgId = orgId
            ).onSuccess { donation ->
                selectedDonation.value = donation
                // Trigger Receipt PDF generation
                _activeOrgDetails.value?.let { org ->
                    val file = PdfGenerator.generateDonationReceiptPdf(getApplication(), donation, org.name)
                    generatedReceiptFile.value = file
                }
                showSuccess("Donation saved successfully! ₹${amount.toInt()}")
                onSuccess(donation)
            }.onFailure {
                showError("Failed to save donation slot")
            }
            _isLoading.value = false
        }
    }

    fun deleteDonation(donation: Donation) {
        viewModelScope.launch {
            _isLoading.value = true
            donationRepo.deleteDonation(donation.id).onSuccess {
                showSuccess("Donation ledger entry removed")
            }.onFailure {
                showError("Error deleting donation slot")
            }
            _isLoading.value = false
        }
    }

    fun generateDirectPdfReceipt(donation: Donation) {
        selectedDonation.value = donation
        val orgName = activeOrgDetails.value?.name ?: "ChandaBook Committee"
        val file = PdfGenerator.generateDonationReceiptPdf(getApplication(), donation, orgName)
        generatedReceiptFile.value = file
    }

    // --- EXPENSES CRUDS ---
    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        notes: String?,
        billImage: String?,
        onSuccess: () -> Unit
    ) {
        val orgId = activeOrgId.value ?: return showError("Please select or create an organization first")
        viewModelScope.launch {
            _isLoading.value = true
            expenseRepo.createExpense(
                title = title,
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                notes = notes,
                billImageUrl = billImage,
                orgId = orgId
            ).onSuccess {
                showSuccess("Expense listed! ₹${amount.toInt()}")
                onSuccess()
            }.onFailure {
                showError("Failed to save expense payload")
            }
            _isLoading.value = false
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            _isLoading.value = true
            expenseRepo.deleteExpense(expense.id).onSuccess {
                showSuccess("Expense ledger entry deleted")
            }.onFailure {
                showError("Error deleting expense")
            }
            _isLoading.value = false
        }
    }

    // --- EXPORT PDF LEDGER REPORT ---
    fun exportLedgerReport(onCompleted: (File) -> Unit) {
        val org = activeOrgDetails.value ?: return showError("No active organization context found")
        val dons = _donations.value
        val exps = _expenses.value
        val stats = summary.value

        viewModelScope.launch {
            _isLoading.value = true
            delay(100) // Yield to main thread for graphics setup
            val file = PdfGenerator.generateLedgerReportPdf(getApplication(), org.name, dons, exps, stats)
            if (file != null) {
                onCompleted(file)
            } else {
                showError("Failed to build PDF ledger print")
            }
            _isLoading.value = false
        }
    }

    // --- ERROR CODES DECODING ---
    private fun handleApiError(e: Throwable) {
        val msg = e.localizedMessage ?: "Unknown connection error"
        if (msg.contains("401") || msg.contains("Unauthorized")) {
            // Unauthorised → clear session & direct to Login
            logoutUser { }
            showError("Session expired. Please log in again.")
        } else if (msg.contains("403")) {
            showError("You do not have permissions for this operation.")
        } else if (msg.contains("404")) {
            showError("Requested resources could not be found.")
        } else if (msg.contains("500") || msg.contains("Server Error")) {
            showError("Server error. Committee data integrity intact, try re-syncing.")
        } else {
            showError("Working offline mode. Displaying cached books.")
        }
    }
}

// Unified Factory to create ViewModel passing dependencies fetched from AppContainer
class ChandaBookViewModelFactory(
    private val application: Application,
    private val authRepo: AuthRepository,
    private val orgRepo: OrgRepository,
    private val donationRepo: DonationRepository,
    private val expenseRepo: ExpenseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChandaBookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChandaBookViewModel(application, authRepo, orgRepo, donationRepo, expenseRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class context")
    }
}
