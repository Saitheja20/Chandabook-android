package com.example.data.repository

import com.example.data.api.ChandaBookApiService
import com.example.data.api.CreateOrgRequest
import com.example.data.local.OrganizationDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.io.IOException

class OrgRepository(
    private val api: ChandaBookApiService,
    private val dao: OrganizationDao,
    private val sessionManager: SessionManager
) {

    // Expose organizations reactively from SQLite
    val organizationsFlow: Flow<List<Organization>> = dao.getAllOrganizationsFlow()

    // Switch current selected organization ID
    fun getActiveOrgId(): String? = sessionManager.currentOrganizationId
    fun setActiveOrgId(orgId: String) {
        sessionManager.currentOrganizationId = orgId
    }

    suspend fun fetchMyOrganizations(): Result<List<Organization>> {
        if (sessionManager.getUserEmail() == "test@ai.com" || sessionManager.token == "eyJkZW1vIjoiY2hhbmRhYm9vayJ9.demo.signature") {
            android.util.Log.d("DEMO_AUTH", "Using offline demo fetchMyOrganizations")
            val demoOrg = Organization(
                id = "demo-org-001",
                name = "AI Studio Demo Committee",
                organizationCode = "DEMO01",
                createdBy = "demo-user-001",
                createdByName = "AI Studio Demo",
                createdAt = "2026-06-10T00:00:00Z",
                updatedAt = "2026-06-10T00:00:00Z"
            )
            dao.insertOrganization(demoOrg)
            return Result.success(listOf(demoOrg))
        }
        return try {
            val orgs = api.getMyOrganizations()
            // Save to database
            dao.clearOrganizations()
            dao.insertOrganizations(orgs)
            Result.success(orgs)
        } catch (e: Exception) {
            // Offline fallback
            val cached = dao.getAllOrganizations()
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun createOrganization(name: String, description: String?): Result<Organization> {
        if (sessionManager.getUserEmail() == "test@ai.com" || sessionManager.token == "eyJkZW1vIjoiY2hhbmRhYm9vayJ9.demo.signature") {
            android.util.Log.d("DEMO_AUTH", "Using offline demo createOrganization")
            val code = "DEMO" + (10..99).random()
            val newOrg = Organization(
                id = "org-" + java.util.UUID.randomUUID().toString().take(8),
                name = name,
                description = description,
                organizationCode = code,
                createdBy = "demo-user-001",
                createdByName = "AI Studio Demo",
                createdAt = "2026-06-10T00:00:00Z",
                updatedAt = "2026-06-10T00:00:00Z"
            )
            dao.insertOrganization(newOrg)
            if (sessionManager.currentOrganizationId == null) {
                sessionManager.currentOrganizationId = newOrg.id
            }
            return Result.success(newOrg)
        }
        return try {
            val newOrg = api.createOrganization(CreateOrgRequest(name, description))
            dao.insertOrganization(newOrg)
            if (sessionManager.currentOrganizationId == null) {
                sessionManager.currentOrganizationId = newOrg.id
            }
            Result.success(newOrg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMembers(orgId: String): Result<List<Member>> {
        if (sessionManager.getUserEmail() == "test@ai.com" || sessionManager.token == "eyJkZW1vIjoiY2hhbmRhYm9vayJ9.demo.signature") {
            val demoMembers = listOf(
                Member(
                    id = "demo-user-001",
                    displayName = "AI Studio Demo",
                    email = "test@ai.com",
                    userRole = "admin",
                    orgRole = "Admin",
                    joinedAt = "2026-06-10T00:00:00Z"
                )
            )
            return Result.success(demoMembers)
        }
        return try {
            val members = api.getOrganizationMembers(orgId)
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrgDetails(orgId: String): Result<Organization> {
        if (orgId == "demo-org-001" || sessionManager.getUserEmail() == "test@ai.com" || sessionManager.token == "eyJkZW1vIjoiY2hhbmRhYm9vayJ9.demo.signature") {
            android.util.Log.d("DEMO_AUTH", "Using offline demo getOrgDetails")
            val demoOrg = Organization(
                id = "demo-org-001",
                name = "AI Studio Demo Committee",
                organizationCode = "DEMO01",
                createdBy = "demo-user-001",
                createdByName = "AI Studio Demo",
                createdAt = "2026-06-10T00:00:00Z",
                updatedAt = "2026-06-10T00:00:00Z"
            )
            dao.insertOrganization(demoOrg)
            return Result.success(demoOrg)
        }
        return try {
            val details = api.getOrganizationDetails(orgId)
            dao.insertOrganization(details)
            Result.success(details)
        } catch (e: Exception) {
            val local = dao.getOrganizationById(orgId)
            if (local != null) {
                Result.success(local)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun updateOrganization(orgId: String, name: String, description: String?): Result<Organization> {
        return try {
            val updated = api.updateOrganization(orgId, CreateOrgRequest(name, description))
            dao.insertOrganization(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPublicOrg(orgCode: String): Result<PublicOrg> {
        return try {
            val publicOrg = api.getPublicOrg(orgCode)
            Result.success(publicOrg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPublicSummary(orgCode: String): Result<PublicSummary> {
        return try {
            val summary = api.getPublicSummary(orgCode)
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPublicDonations(orgCode: String): Result<List<PublicDonation>> {
        return try {
            val donations = api.getPublicDonations(orgCode)
            Result.success(donations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPublicExpenses(orgCode: String): Result<List<PublicExpense>> {
        return try {
            val expenses = api.getPublicExpenses(orgCode)
            Result.success(expenses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
