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
                    android.util.Log.d("AppContainer", "OkHttp Interceptor: Attaching Authorization header (Bearer token prefix, length=${jwtToken.length})")
                    requestBuilder.header("Authorization", "Bearer $jwtToken")
                } else {
                    android.util.Log.d("AppContainer", "OkHttp Interceptor: No token found in SessionManager, requesting anonymously/guest context")
                }
                
                val request = requestBuilder.build()
                android.util.Log.d("AppContainer", "OkHttp Request: [${request.method}] -> ${request.url}")
                var response: okhttp3.Response? = null
                var primaryException: Exception? = null
                
                try {
                    response = chain.proceed(request)
                } catch (e: Exception) {
                    primaryException = e
                    android.util.Log.e("AppContainer", "OkHttp Request Failed with exception for ${request.url}", e)
                }
                
                // Check if primary call succeeded and we got a 401
                if (response != null) {
                    android.util.Log.d("AppContainer", "OkHttp Response: Code=${response.code} for URL=${request.url}")
                    if (response.code == 401) {
                        android.util.Log.w("AppContainer", "OkHttp Response 401: Unauthorized response detected!")
                        if (!jwtToken.isNullOrEmpty()) {
                            android.util.Log.e("AppContainer", "OkHttp 401: Active JWT token rejected by backend. Clearing session & triggering expiry handler.")
                            sessionManager.clear()
                            sessionManager.onSessionExpired?.invoke()
                        } else {
                            android.util.Log.d("AppContainer", "OkHttp 401: Received 401 but no JWT was supplied anyway. Skipping session clearance behavior.")
                        }
                    }
                    response
                } else {
                    // Try the direct IP fallback URL as secondary if primary had an SSL or timeout exception
                    val host = request.url.host
                    val isSslOrTimeout = primaryException != null && (
                        primaryException is javax.net.ssl.SSLException ||
                        primaryException is java.net.SocketTimeoutException ||
                        primaryException is java.net.ConnectException ||
                        primaryException is java.net.UnknownHostException ||
                        primaryException is java.io.IOException ||
                        primaryException.message?.contains("ssl", ignoreCase = true) == true ||
                        primaryException.message?.contains("timeout", ignoreCase = true) == true ||
                        primaryException.message?.contains("time out", ignoreCase = true) == true
                    )

                    if (host == "api.chandabook.com" && isSslOrTimeout) {
                        val fallbackUrl = request.url.newBuilder()
                            .scheme("http")
                            .host("129.159.23.12")
                            .port(3000)
                            .build()
                        val fallbackRequest = request.newBuilder()
                            .url(fallbackUrl)
                            .build()
                        android.util.Log.w("AppContainer", "OkHttp Fallback: Primary failed. Retrying with fallback IP: $fallbackUrl (retaining attached Auth headers)")
                        try {
                            val fallbackResponse = chain.proceed(fallbackRequest)
                            android.util.Log.d("AppContainer", "OkHttp Fallback Response: Code=${fallbackResponse.code}")
                            if (fallbackResponse.code == 401) {
                                android.util.Log.w("AppContainer", "OkHttp Fallback Response 401: Unauthorized response detected!")
                                if (!jwtToken.isNullOrEmpty()) {
                                    android.util.Log.e("AppContainer", "OkHttp Fallback 401: Active JWT token rejected by fallback. Clearing session.")
                                    sessionManager.clear()
                                    sessionManager.onSessionExpired?.invoke()
                                }
                            }
                            fallbackResponse
                        } catch (fallbackEx: Exception) {
                            android.util.Log.e("AppContainer", "OkHttp Fallback Failed", fallbackEx)
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
