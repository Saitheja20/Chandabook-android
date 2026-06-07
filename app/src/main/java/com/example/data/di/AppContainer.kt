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
import java.io.IOException
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
                
                val request = requestBuilder.build()
                var response: okhttp3.Response? = null
                var primaryException: Exception? = null
                
                try {
                    response = chain.proceed(request)
                } catch (e: Exception) {
                    primaryException = e
                }
                
                // Check if primary call succeeded and we got a 401
                if (response != null) {
                    if (response.code == 401) {
                        sessionManager.clear()
                        sessionManager.onSessionExpired?.invoke()
                    }
                    response
                } else {
                    // Try the direct IP fallback URL as secondary
                    val host = request.url.host
                    if (host == "chandabook.com") {
                        val fallbackUrl = request.url.newBuilder()
                            .scheme("http")
                            .host("129.159.23.12")
                            .port(3000)
                            .build()
                        val fallbackRequest = request.newBuilder()
                            .url(fallbackUrl)
                            .build()
                        try {
                            val fallbackResponse = chain.proceed(fallbackRequest)
                            if (fallbackResponse.code == 401) {
                                sessionManager.clear()
                                sessionManager.onSessionExpired?.invoke()
                            }
                            fallbackResponse
                        } catch (fallbackEx: Exception) {
                            throw primaryException ?: fallbackEx
                        }
                    } else {
                        throw primaryException ?: IOException("Request failed without response")
                    }
                }
            }
            .build()
    }

    private val apiService: ChandaBookApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://chandabook.com/api/")
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
