package com.example.data.di

import android.content.Context
import androidx.room.Room
import com.example.data.api.ChandaBookApiService
import com.example.data.local.AppDatabase
import com.example.data.repository.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val sessionManager: SessionManager
    val authRepository: AuthRepository
    val orgRepository: OrgRepository
    val donationRepository: DonationRepository
    val expenseRepository: ExpenseRepository
}

class AppContainerImpl(private val context: Context) : AppContainer {

    override val sessionManager: SessionManager by lazy {
        SessionManager(context)
    }

    private val db: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "chandabook.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                
                // Read JWT Token from SessionManager and append to headers
                val jwtToken = sessionManager.token
                if (!jwtToken.isNullOrEmpty()) {
                    requestBuilder.header("Authorization", "Bearer $jwtToken")
                }
                
                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    private val apiService: ChandaBookApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.chandabook.com/api/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ChandaBookApiService::class.java)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepository(apiService, sessionManager)
    }

    override val orgRepository: OrgRepository by lazy {
        OrgRepository(apiService, db.organizationDao(), sessionManager)
    }

    override val donationRepository: DonationRepository by lazy {
        DonationRepository(apiService, db.donationDao(), sessionManager)
    }

    override val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepository(apiService, db.expenseDao(), sessionManager)
    }
}
