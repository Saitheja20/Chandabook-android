package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Authentication schemas
data class User(
    val id: String = "",
    val displayName: String? = null,
    val name: String? = "",
    val email: String? = null,
    val phone: String? = null,
    val role: String? = "viewer",
    val auth_method: String? = "google",
    val avatar_url: String? = null,
    val created_at: String? = null,
    val last_login_at: String? = null,
    val organizations: List<UserOrganization> = emptyList()
)

data class UserOrganization(
    val org_id: String = "",
    val org_name: String = "",
    val org_type: String? = null,
    val role: String = "viewer" // e.g. "admin", "member", "viewer"
)

// Main domain entities representing community assets and cash books

@Entity(tableName = "organizations")
data class Organization(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val description: String? = null,
    val organizationCode: String = "",
    val createdBy: String = "",
    val createdByName: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(tableName = "donations")
data class Donation(
    @PrimaryKey val id: String = "",
    val donorName: String = "",
    val phoneNumber: String? = null,
    val amount: Double = 0.0,
    val category: String = "", // "donation", "prasad", "decoration", "sound", "pooja", "other"
    val paymentMethod: String = "", // "cash", "upi", "bank_transfer", "cheque", "online"
    val receiptNumber: String = "",
    val address: String? = null,
    val notes: String? = null,
    val organizationId: String = "",
    val chandaBookId: String = "",
    val receivedBy: String = "",
    val receivedByName: String = "",
    val createdAt: String = "",
    val isSynced: Boolean = true // Local offline tracking flag
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "", // "decoration", "pooja_items", "sound", "prasad", "printing", "transport", "other"
    val paymentMethod: String = "", // "cash", "upi", "bank_transfer", "cheque", "online"
    val billImageUrl: String? = null,
    val notes: String? = null,
    val organizationId: String = "",
    val chandaBookId: String = "",
    val addedBy: String = "",
    val addedByName: String = "",
    val createdAt: String = "",
    val isSynced: Boolean = true // Local offline tracking flag
)

data class Member(
    val id: String = "",
    val displayName: String = "",
    val email: String? = null,
    val phone: String? = null, // optional
    val userRole: String? = null,
    val orgRole: String = "Viewer", // "Admin", "Member", "Viewer"
    val joinedAt: String? = null
)

// Statistics and Analytical Summary Models
data class SummaryTotals(
    val totalDonations: Int = 0,
    val totalAmount: Double = 0.0,
    val avgAmount: Double = 0.0,
    val maxAmount: Double = 0.0,
    val byCategory: List<CategoryDetail> = emptyList(),
    val byPayment: List<PaymentDetail> = emptyList()
)

data class CategoryDetail(
    val category: String = "",
    val count: Int = 0,
    val total: Double = 0.0
)

data class PaymentDetail(
    val paymentMethod: String = "",
    val count: Int = 0,
    val total: Double = 0.0
)

data class PublicOrg(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val orgCode: String? = null,
    val createdByName: String? = null,
    val createdAt: String = ""
)

data class PublicSummary(
    val totalDonations: Int = 0,
    val totalAmount: Double = 0.0,
    val totalExpenses: Int = 0,
    val totalExpenseAmount: Double = 0.0,
    val netBalance: Double = 0.0
)

data class PublicDonation(
    val donorName: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val paymentMethod: String = "",
    val receiptNumber: String = "",
    val receivedByName: String = "",
    val createdAt: String = ""
)

data class PublicExpense(
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val paymentMethod: String = "",
    val addedByName: String = "",
    val createdAt: String = ""
)
