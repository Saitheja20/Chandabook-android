package com.example.data.repository

import com.example.data.api.ChandaBookApiService
import com.example.data.local.ExpenseDao
import com.example.data.model.Expense
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ExpenseRepository(
    private val api: ChandaBookApiService,
    private val dao: ExpenseDao,
    private val sessionManager: SessionManager
) {

    // Return live expenses list from DB
    fun getExpensesFlow(orgId: String): Flow<List<Expense>> {
        return dao.getExpensesByOrgFlow(orgId)
    }

    suspend fun refreshExpenses(orgId: String): Result<List<Expense>> {
        return try {
            val list = api.getExpenses(orgId, null, null, null)
            // Cache in Room
            dao.insertExpenses(list.map { it.copy(isSynced = true) })
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createExpense(
        title: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        notes: String?,
        billImageUrl: String?,
        orgId: String
    ): Result<Expense> {
        val user = sessionManager.user
        val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

        val newExpense = Expense(
            id = UUID.randomUUID().toString(),
            title = title,
            amount = amount,
            category = category,
            paymentMethod = paymentMethod,
            billImageUrl = billImageUrl,
            notes = notes,
            organizationId = orgId,
            chandaBookId = "current_book",
            addedBy = user?.id ?: "unknown",
            addedByName = user?.name ?: "Self",
            createdAt = dateStr,
            isSynced = false // Initially false, synced back in background
        )

        // Optimistic save in SQLite immediately
        dao.insertExpense(newExpense)

        return try {
            val created = api.createExpense(newExpense)
            // Update SQLite entry as synced
            dao.insertExpense(created.copy(isSynced = true))
            Result.success(created)
        } catch (e: Exception) {
            // Keep local version with isSynced = false
            Result.success(newExpense)
        }
    }

    suspend fun updateExpense(expense: Expense): Result<Expense> {
        val pending = expense.copy(isSynced = false)
        dao.insertExpense(pending)

        return try {
            val updated = api.updateExpense(expense.id, expense)
            dao.insertExpense(updated.copy(isSynced = true))
            Result.success(updated)
        } catch (e: Exception) {
            Result.success(pending)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return try {
            api.deleteExpense(expenseId)
            dao.deleteExpenseById(expenseId)
            Result.success(Unit)
        } catch (e: Exception) {
            // Delete locally as optimistic operation
            dao.deleteExpenseById(expenseId)
            Result.success(Unit)
        }
    }

    suspend fun getExpenseDetails(expenseId: String): Result<Expense> {
        return try {
            val expense = api.getExpense(expenseId)
            dao.insertExpense(expense.copy(isSynced = true))
            Result.success(expense)
        } catch (e: Exception) {
            val local = dao.getExpenseById(expenseId)
            if (local != null) {
                Result.success(local)
            } else {
                Result.failure(e)
            }
        }
    }

    // Trigger sync for unsynced expenses
    suspend fun syncUnsyncedExpenses(): Int {
        val unsyncedList = dao.getUnsyncedExpenses()
        var successCount = 0
        for (expense in unsyncedList) {
            try {
                api.createExpense(expense)
                dao.insertExpense(expense.copy(isSynced = true))
                successCount++
            } catch (e: Exception) {
                // Let next pass retry
            }
        }
        return successCount
    }
}
