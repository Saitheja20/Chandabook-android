package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Authentication schemas
data class User(
    val id: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val auth_method: String,
    val avatar_url: String?,
    val created_at: String?,
    val last_login_at: String?,
    val organizations: List<UserOrganization> = emptyList()
)

data class UserOrganization(
    val org_id: String,
    val org_name: String,
    val org_type: String?,
    val role: String // e.g. "admin", "member", "viewer"
)

// Main domain entities representing community assets and cash books

@Entity(tableName = "organizations")
data class Organization(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val organizationCode: String,
    val createdBy: String,
    val createdByName: String?,
    val createdAt: String,
    val updatedAt: String
)

@Entity(tableName = "donations")
data class Donation(
    @PrimaryKey val id: String,
    val donorName: String,
    val phoneNumber: String?,
    val amount: Double,
    val category: String, // "donation", "prasad", "decoration", "sound", "pooja", "other"
    val paymentMethod: String, // "cash", "upi", "bank_transfer", "cheque", "online"
    val receiptNumber: String,
    val address: String?,
    val notes: String?,
    val organizationId: String,
    val chandaBookId: String,
    val receivedBy: String,
    val receivedByName: String,
    val createdAt: String,
    val isSynced: Boolean = true // Local offline tracking flag
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val category: String, // "decoration", "pooja_items", "sound", "prasad", "printing", "transport", "other"
    val paymentMethod: String, // "cash", "upi", "bank_transfer", "cheque", "online"
    val billImageUrl: String?,
    val notes: String?,
    val organizationId: String,
    val chandaBookId: String,
    val addedBy: String,
    val addedByName: String,
    val createdAt: String,
    val isSynced: Boolean = true // Local offline tracking flag
)

data class Member(
    val id: String,
    val displayName: String,
    val email: String?,
    val phone: String?, // optional
    val userRole: String?,
    val orgRole: String, // "Admin", "Member", "Viewer"
    val joinedAt: String?
)

// Statistics and Analytical Summary Models
data class SummaryTotals(
    val totalDonations: Int,
    val totalAmount: Double,
    val avgAmount: Double,
    val maxAmount: Double,
    val byCategory: List<CategoryDetail> = emptyList(),
    val byPayment: List<PaymentDetail> = emptyList()
)

data class CategoryDetail(
    val category: String,
    val count: Int,
    val total: Double
)

data class PaymentDetail(
    val paymentMethod: String,
    val count: Int,
    val total: Double
)
