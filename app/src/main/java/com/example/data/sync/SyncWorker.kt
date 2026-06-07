package com.example.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ChandaBookApplication

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val application = applicationContext as? ChandaBookApplication ?: return Result.failure()
        val container = application.container

        return try {
            val donationCount = container.donationRepository.syncUnsyncedDonations()
            val expenseCount = container.expenseRepository.syncUnsyncedExpenses()
            
            if (donationCount > 0 || expenseCount > 0) {
                // If synced some objects, trigger a refresh of cache
                val currentOrgId = container.orgRepository.getActiveOrgId()
                if (currentOrgId != null) {
                    container.donationRepository.refreshDonations(currentOrgId)
                    container.expenseRepository.refreshExpenses(currentOrgId)
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
