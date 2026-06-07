package com.example.data.local

import androidx.room.*
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.Organization
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizationDao {
    @Query("SELECT * FROM organizations ORDER BY name ASC")
    fun getAllOrganizationsFlow(): Flow<List<Organization>>

    @Query("SELECT * FROM organizations")
    suspend fun getAllOrganizations(): List<Organization>

    @Query("SELECT * FROM organizations WHERE id = :id")
    suspend fun getOrganizationById(id: String): Organization?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrganizations(organizations: List<Organization>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrganization(organization: Organization)

    @Query("DELETE FROM organizations")
    suspend fun clearOrganizations()
}

@Dao
interface DonationDao {
    @Query("SELECT * FROM donations WHERE organizationId = :orgId ORDER BY createdAt DESC")
    fun getDonationsByOrgFlow(orgId: String): Flow<List<Donation>>

    @Query("SELECT * FROM donations WHERE id = :id")
    suspend fun getDonationById(id: String): Donation?

    @Query("SELECT * FROM donations WHERE isSynced = 0")
    suspend fun getUnsyncedDonations(): List<Donation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonations(donations: List<Donation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonation(donation: Donation)

    @Update
    suspend fun updateDonation(donation: Donation)

    @Query("DELETE FROM donations WHERE id = :id")
    suspend fun deleteDonationById(id: String)

    @Query("DELETE FROM donations")
    suspend fun clearDonations()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE organizationId = :orgId ORDER BY createdAt DESC")
    fun getExpensesByOrgFlow(orgId: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: String): Expense?

    @Query("SELECT * FROM expenses WHERE isSynced = 0")
    suspend fun getUnsyncedExpenses(): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()
}

@Database(
    entities = [Organization::class, Donation::class, Expense::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun organizationDao(): OrganizationDao
    abstract fun donationDao(): DonationDao
    abstract fun expenseDao(): ExpenseDao
}
