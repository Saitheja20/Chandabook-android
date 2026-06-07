package com.example.data.repository

import com.example.data.api.ChandaBookApiService
import com.example.data.api.CreateOrgRequest
import com.example.data.local.OrganizationDao
import com.example.data.model.Member
import com.example.data.model.Organization
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
        return try {
            val members = api.getOrganizationMembers(orgId)
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrgDetails(orgId: String): Result<Organization> {
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
}
