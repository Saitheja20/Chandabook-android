package com.example.data.repository

import com.example.data.api.ChandaBookApiService
import com.example.data.api.DonationsResponse
import com.example.data.api.SummaryTotalsResponse
import com.example.data.local.DonationDao
import com.example.data.model.CategoryDetail
import com.example.data.model.Donation
import com.example.data.model.PaymentDetail
import com.example.data.model.SummaryTotals
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DonationRepository(
    private val api: ChandaBookApiService,
    private val dao: DonationDao,
    private val sessionManager: SessionManager
) {

    // Return live donations list from DB
    fun getDonationsFlow(orgId: String): Flow<List<Donation>> {
        return dao.getDonationsByOrgFlow(orgId)
    }

    suspend fun refreshDonations(orgId: String): Result<List<Donation>> {
        if (sessionManager.getUserEmail() == "test@ai.com" || sessionManager.token == "eyJkZW1vIjoiY2hhbmRhYm9vayJ9.demo.signature" || orgId == "demo-org-001") {
            android.util.Log.d("DEMO_AUTH", "Using offline demo refreshDonations")
            return Result.success(emptyList())
        }
        return try {
            val response = api.getDonations(
                organizationId = orgId,
                chandaBookId = null,
                category = null,
                paymentMethod = null,
                startDate = null,
                endDate = null,
                minAmount = null,
                maxAmount = null,
                search = null,
                page = 1,
                limit = 300
            )
            val list = response.data
            // Cache in Room
            dao.insertDonations(list.map { it.copy(isSynced = true) })
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDonationDetails(donationId: String): Result<Donation> {
        return try {
            val donation = api.getDonation(donationId)
            dao.insertDonation(donation.copy(isSynced = true))
            Result.success(donation)
        } catch (e: Exception) {
            val local = dao.getDonationById(donationId)
            if (local != null) {
                Result.success(local)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun createDonation(
        donorName: String,
        phoneNumber: String?,
        amount: Double,
        category: String,
        paymentMethod: String,
        address: String?,
        notes: String?,
        receiptNumber: String,
        orgId: String
    ): Result<Donation> {
        val user = sessionManager.user
        val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

        val newDonation = Donation(
            id = UUID.randomUUID().toString(),
            donorName = donorName,
            phoneNumber = phoneNumber,
            amount = amount,
            category = category,
            paymentMethod = paymentMethod,
            receiptNumber = receiptNumber,
            address = address,
            notes = notes,
            organizationId = orgId,
            chandaBookId = "current_book",
            receivedBy = user?.id ?: "unknown",
            receivedByName = user?.name ?: "Self",
            createdAt = dateStr,
            isSynced = false // Initially false, synced back in background
        )

        // Optimistic save in SQLite immediately
        dao.insertDonation(newDonation)

        if (sessionManager.getUserEmail() == "test@ai.com" || sessionManager.token == "eyJkZW1vIjoiY2hhbmRhYm9vayJ9.demo.signature" || orgId == "demo-org-001") {
            android.util.Log.d("DEMO_AUTH", "Using offline demo createDonation")
            dao.insertDonation(newDonation.copy(isSynced = true))
            return Result.success(newDonation.copy(isSynced = true))
        }

        return try {
            val created = api.createDonation(newDonation)
            // Update SQLite entry as synced
            dao.insertDonation(created.copy(isSynced = true))
            Result.success(created)
        } catch (e: Exception) {
            // Offline Mode - Keep local version with isSynced = false
            Result.success(newDonation)
        }
    }

    suspend fun updateDonation(donation: Donation): Result<Donation> {
        // Save modified copy locally
        val pending = donation.copy(isSynced = false)
        dao.insertDonation(pending)

        return try {
            val updated = api.updateDonation(donation.id, donation)
            dao.insertDonation(updated.copy(isSynced = true))
            Result.success(updated)
        } catch (e: Exception) {
            // If offline, preserve modified state offline
            Result.success(pending)
        }
    }

    suspend fun deleteDonation(donationId: String): Result<Unit> {
        return try {
            api.deleteDonation(donationId)
            dao.deleteDonationById(donationId)
            Result.success(Unit)
        } catch (e: Exception) {
            // Delete locally as optimistic operation
            dao.deleteDonationById(donationId)
            Result.success(Unit)
        }
    }

    suspend fun getSummaryTotals(orgId: String): Result<SummaryTotals> {
        if (sessionManager.getUserEmail() == "test@ai.com" || sessionManager.token == "eyJkZW1vIjoiY2hhbmRhYm9vayJ9.demo.signature" || orgId == "demo-org-001") {
            android.util.Log.d("DEMO_AUTH", "Using offline demo getSummaryTotals")
            return Result.success(
                SummaryTotals(
                    totalDonations = 12,
                    totalAmount = 78500.0,
                    avgAmount = 6541.67,
                    maxAmount = 25000.0,
                    byCategory = listOf(
                        CategoryDetail("decoration", 3, 15000.0),
                        CategoryDetail("pooja_items", 5, 28500.0),
                        CategoryDetail("sound", 2, 15000.0),
                        CategoryDetail("prasad", 2, 20000.0)
                    ),
                    byPayment = listOf(
                        PaymentDetail("cash", 4, 18500.0),
                        PaymentDetail("upi", 6, 45000.0),
                        PaymentDetail("bank_transfer", 2, 15000.0)
                    )
                )
            )
        }
        return try {
            val response = api.getSummaryTotals(orgId, null, null, null)
            val stats = SummaryTotals(
                totalDonations = response.summary?.totalDonations ?: 0,
                totalAmount = response.summary?.totalAmount ?: 0.0,
                avgAmount = response.summary?.avgAmount ?: 0.0,
                maxAmount = response.summary?.maxAmount ?: 0.0,
                byCategory = response.byCategory,
                byPayment = response.byPayment
            )
            Result.success(stats)
        } catch (e: Exception) {
            // Aggregate from SQLite for seamless offline reporting!
            val list = mutableListOf<Donation>()
            dao.getDonationsByOrgFlow(orgId).collect {
                list.addAll(it)
            }
            if (list.isEmpty()) {
                return Result.failure(e)
            }
            // Offline aggregation
            val totalAmount = list.sumOf { it.amount }
            val avg = if (list.isNotEmpty()) totalAmount / list.size else 0.0
            val max = if (list.isNotEmpty()) list.maxOf { it.amount } else 0.0

            val byCat = list.groupBy { it.category }.map { (cat, items) ->
                CategoryDetail(cat, items.size, items.sumOf { it.amount })
            }
            val byPay = list.groupBy { it.paymentMethod }.map { (pay, items) ->
                PaymentDetail(pay, items.size, items.sumOf { it.amount })
            }

            Result.success(
                SummaryTotals(
                    totalDonations = list.size,
                    totalAmount = totalAmount,
                    avgAmount = avg,
                    maxAmount = max,
                    byCategory = byCat,
                    byPayment = byPay
                )
            )
        }
    }

    // Trigger sync for unsynced donations
    suspend fun syncUnsyncedDonations(): Int {
        val unsyncedList = dao.getUnsyncedDonations()
        var successCount = 0
        for (donation in unsyncedList) {
            try {
                api.createDonation(donation)
                dao.insertDonation(donation.copy(isSynced = true))
                successCount++
            } catch (e: Exception) {
                // Ignore failure for individual records, let next pass retry
            }
        }
        return successCount
    }
}
