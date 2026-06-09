package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

data class TestResult(
    val testNumber: Int,
    val testName: String,
    val status: TestStatus,  // PENDING, RUNNING, PASS, FAIL
    val httpCode: Int? = null,
    val responseTimeMs: Long? = null,
    val responseBody: String? = null,
    val errorMessage: String? = null
)

enum class TestStatus { PENDING, RUNNING, PASS, FAIL }

class DiagnosticViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<TestResult>>(emptyList())
    val results: StateFlow<List<TestResult>> = _results.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    init {
        initializeTestList()
    }

    fun initializeTestList() {
        val initialList = listOf(
            TestResult(1, "Internet Connectivity", TestStatus.PENDING),
            TestResult(2, "HTTPS Domain Reachable (api.chandabook.com)", TestStatus.PENDING),
            TestResult(3, "HTTP Direct IP Reachable (129.159.23.12)", TestStatus.PENDING),
            TestResult(4, "API Base Endpoint (HTTPS)", TestStatus.PENDING),
            TestResult(5, "API Base Endpoint (Direct IP)", TestStatus.PENDING),
            TestResult(6, "Auth Endpoint Exists (Email OTP)", TestStatus.PENDING),
            TestResult(7, "Auth Endpoint Exists (Google)", TestStatus.PENDING),
            TestResult(8, "Auth Endpoint Exists (Login Email)", TestStatus.PENDING),
            TestResult(9, "Public Org Lookup", TestStatus.PENDING),
            TestResult(10, "Organizations (Auth Required)", TestStatus.PENDING),
            TestResult(11, "Donations Endpoint Exists", TestStatus.PENDING),
            TestResult(12, "SSL Certificate Check (api.chandabook.com)", TestStatus.PENDING)
        )
        _results.value = initialList
    }

    private fun updateTestResult(testNumber: Int, updater: (TestResult) -> TestResult) {
        _results.value = _results.value.map {
            if (it.testNumber == testNumber) updater(it) else it
        }
    }

    fun runDiagnostics(context: Context) {
        if (_isTesting.value) return
        _isTesting.value = true

        // Set all to PENDING
        initializeTestList()

        viewModelScope.launch {
            val jobs = listOf(
                async { executeTest1(context) },
                async { executeTest2() },
                async { executeTest3() },
                async { executeTest4() },
                async { executeTest5() },
                async { executeTest6() },
                async { executeTest7() },
                async { executeTest8() },
                async { executeTest9() },
                async { executeTest10() },
                async { executeTest11() },
                async { executeTest12() }
            )
            // Wait for all to complete
            jobs.awaitAll()
            _isTesting.value = false
        }
    }

    private suspend fun executeTestWithTimeout(
        testNumber: Int,
        testLogic: suspend () -> TestResult
    ) {
        updateTestResult(testNumber) { it.copy(status = TestStatus.RUNNING) }
        val result = try {
            withTimeout(10000L) {
                testLogic()
            }
        } catch (e: Exception) {
            TestResult(
                testNumber = testNumber,
                testName = _results.value.first { it.testNumber == testNumber }.testName,
                status = TestStatus.FAIL,
                errorMessage = e.localizedMessage ?: "Timeout or execution exception"
            )
        }
        updateTestResult(testNumber) { result }
    }

    private suspend fun executeTest1(context: Context) {
        executeTestWithTimeout(1) {
            val startTime = System.currentTimeMillis()
            var hasInternet = false
            withContext(Dispatchers.IO) {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val nw = cm.activeNetwork
                    val actNw = cm.getNetworkCapabilities(nw)
                    if (actNw != null) {
                        hasInternet = actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    }
                } else {
                    val nwInfo = cm.activeNetworkInfo
                    hasInternet = nwInfo != null && nwInfo.isConnected
                }

                if (hasInternet) {
                    // Quick socket connection check to make sure
                    try {
                        val socket = java.net.Socket()
                        socket.connect(java.net.InetSocketAddress("google.com", 80), 3000)
                        socket.close()
                        hasInternet = true
                    } catch (e: Exception) {
                        // Keep ConnectivityManager result as fallback
                    }
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            if (hasInternet) {
                TestResult(1, "Internet Connectivity", TestStatus.PASS, responseTimeMs = elapsed, responseBody = "Device is connected to the internet")
            } else {
                TestResult(1, "Internet Connectivity", TestStatus.FAIL, errorMessage = "No internet connection")
            }
        }
    }

    private suspend fun executeTest2() {
        executeTestWithTimeout(2) {
            val startTime = System.currentTimeMillis()
            var code: Int? = null
            var bodyText: String? = null
            var err: String? = null
            var status = TestStatus.FAIL

            withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder().url("https://api.chandabook.com/").get().build()
                    okHttpClient.newCall(request).execute().use { response ->
                        code = response.code
                        bodyText = response.body?.string()?.take(300)
                        status = TestStatus.PASS
                    }
                } catch (e: Exception) {
                    err = e.localizedMessage ?: e.javaClass.simpleName
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            TestResult(2, "HTTPS Domain Reachable (api.chandabook.com)", status, code, elapsed, bodyText, err)
        }
    }

    private suspend fun executeTest3() {
        executeTestWithTimeout(3) {
            val startTime = System.currentTimeMillis()
            var code: Int? = null
            var bodyText: String? = null
            var err: String? = null
            var status = TestStatus.FAIL

            withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder().url("http://129.159.23.12:3000/").get().build()
                    okHttpClient.newCall(request).execute().use { response ->
                        code = response.code
                        bodyText = response.body?.string()?.take(300)
                        status = TestStatus.PASS
                    }
                } catch (e: Exception) {
                    err = "Direct IP unreachable — server may be down"
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            TestResult(3, "HTTP Direct IP Reachable (129.159.23.12)", status, code, elapsed, bodyText, err)
        }
    }

    private suspend fun executeTest4() {
        executeTestWithTimeout(4) {
            val startTime = System.currentTimeMillis()
            var code: Int? = null
            var bodyText: String? = null
            var err: String? = null
            var status = TestStatus.FAIL

            withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder().url("https://api.chandabook.com/api/").get().build()
                    okHttpClient.newCall(request).execute().use { response ->
                        code = response.code
                        bodyText = response.body?.string()?.take(300)
                        if (code == 200 || code == 404) {
                            status = TestStatus.PASS
                        }
                    }
                } catch (e: Exception) {
                    err = e.localizedMessage
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            TestResult(4, "API Base Endpoint (HTTPS)", status, code, elapsed, bodyText, err)
        }
    }

    private suspend fun executeTest5() {
        executeTestWithTimeout(5) {
            val startTime = System.currentTimeMillis()
            var code: Int? = null
            var bodyText: String? = null
            var err: String? = null
            var status = TestStatus.FAIL

            withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder().url("http://129.159.23.12:3000/api/").get().build()
                    okHttpClient.newCall(request).execute().use { response ->
                        code = response.code
                        bodyText = response.body?.string()?.take(300)
                        if (code == 200 || code == 404) {
                            status = TestStatus.PASS
                        }
                    }
                } catch (e: Exception) {
                    err = e.localizedMessage
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            TestResult(5, "API Base Endpoint (Direct IP)", status, code, elapsed, bodyText, err)
        }
    }

    private suspend fun executeTest6() {
        executeTestWithTimeout(6) {
            val startTime = System.currentTimeMillis()
            var code: Int? = null
            var bodyText: String? = null
            var err: String? = null
            var status = TestStatus.FAIL

            withContext(Dispatchers.IO) {
                try {
                    val jsonMediaType = "application/json; charset=utf-8".toMediaType()
                    val payload = """
                        {
                          "email": "test@diagnostic.com",
                          "name": "Diagnostic Test",
                          "orgName": "Test Org",
                          "role": "admin"
                        }
                    """.trimIndent()
                    val request = Request.Builder()
                        .url("https://api.chandabook.com/api/auth/register/email")
                        .post(payload.toRequestBody(jsonMediaType))
                        .build()
                    okHttpClient.newCall(request).execute().use { response ->
                        code = response.code
                        bodyText = response.body?.string()?.take(300)
                        if (code == 200 || code == 201) {
                            status = TestStatus.PASS
                        } else {
                            err = "status error"
                        }
                    }
                } catch (e: Exception) {
                    err = e.localizedMessage ?: e.javaClass.simpleName
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            TestResult(6, "Auth Endpoint Exists (Email OTP)", status, code, elapsed, bodyText, err)
        }
    }

    private suspend fun executeTest7() {
        executeTestWithTimeout(7) {
            val startTime = System.currentTimeMillis()
            var code: Int? = null
            var bodyText: String? = null
            var err: String? = null
            var status = TestStatus.FAIL

            withContext(Dispatchers.IO) {
                try {
                    val jsonMediaType = "application/json; charset=utf-8".toMediaType()
                    val payload = """{"idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjIifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJzdWIiOiIxMjM0NTY3OTAxIn0.dummy_signature_for_format_validation"}"""
                    val request = Request.Builder()
                        .url("https://api.chandabook.com/api/auth/google")
                        .post(payload.toRequestBody(jsonMediaType))
                        .build()
                    okHttpClient.newCall(request).execute().use { response ->
                        code = response.code
                        bodyText = response.body?.string()?.take(300)
                        if (code == 400 || code == 401) {
                            status = TestStatus.PASS
                        }
                    }
                } catch (e: Exception) {
                    err = e.localizedMessage ?: e.javaClass.simpleName
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            TestResult(7, "Auth Endpoint Exists (Google)", status, code, elapsed, bodyText, err)
        }
    }

    private suspend fun executeTest8() {
        executeTestWithTimeout(8) {
            val startTime = System.currentTimeMillis()
            var code: Int? = null
            var bodyText: String? = null
            var err: String? = null
            var status = TestStatus.FAIL

            withContext(Dispatchers.IO) {
                try {
                    val jsonMediaType = "application/json; charset=utf-8".toMediaType()
                    val payload = """{"email": "test@diagnostic.com"}"""
                    val request = Request.Builder()
                        .url("https://api.chandabook.com/api/auth/login/email")
                        .post(payload.toRequestBody(jsonMediaType))
                        .build()
                    okHttpClient.newCall(request).execute().use { response ->
                        code = response.code
                        bodyText = response.body?.string()?.take(300)
                        if (code == 200 || code == 400 || code == 404) {
                            status = TestStatus.PASS
                        }
                    }
                } catch (e: Exception) {
                    err = e.localizedMessage
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            TestResult(8, "Auth Endpoint Exists (Login Email)", status, code, elapsed, bodyText, err)
        }
    }

    private suspend fun executeTest9() {
        executeTestWithTimeout(9) {
            val startTime = System.currentTimeMillis()
            var code: Int? = null
            var bodyText: String? = null
            var err: String? = null
            var status = TestStatus.FAIL

            withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url("https://api.chandabook.com/api/organizations/public/C6UIMS")
                        .get()
                        .build()
                    okHttpClient.newCall(request).execute().use { response ->
                        code = response.code
                        bodyText = response.body?.string()?.take(300)
                        if (code == 200) {
                            status = TestStatus.PASS
                        }
                    }
                } catch (e: Exception) {
                    err = e.localizedMessage
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            TestResult(9, "Public Org Lookup", status, code, elapsed, bodyText, err)
        }
    }

    private suspend fun executeTest10() {
        executeTestWithTimeout(10) {
            val startTime = System.currentTimeMillis()
            var code: Int? = null
            var bodyText: String? = null
            var err: String? = null
            var status = TestStatus.FAIL

            withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url("https://api.chandabook.com/api/organizations")
                        .header("Authorization", "Bearer diagnostic_test")
                        .get()
                        .build()
                    okHttpClient.newCall(request).execute().use { response ->
                        code = response.code
                        bodyText = response.body?.string()?.take(300)
                        if (code == 401) {
                            status = TestStatus.PASS
                        }
                    }
                } catch (e: Exception) {
                    err = e.localizedMessage
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            TestResult(10, "Organizations (Auth Required)", status, code, elapsed, bodyText, err)
        }
    }

    private suspend fun executeTest11() {
        executeTestWithTimeout(11) {
            val startTime = System.currentTimeMillis()
            var code: Int? = null
            var bodyText: String? = null
            var err: String? = null
            var status = TestStatus.FAIL

            withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url("https://api.chandabook.com/api/donations")
                        .header("Authorization", "Bearer diagnostic_test")
                        .get()
                        .build()
                    okHttpClient.newCall(request).execute().use { response ->
                        code = response.code
                        bodyText = response.body?.string()?.take(300)
                        if (code == 401) {
                            status = TestStatus.PASS
                        }
                    }
                } catch (e: Exception) {
                    err = e.localizedMessage
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            TestResult(11, "Donations Endpoint Exists", status, code, elapsed, bodyText, err)
        }
    }

    private suspend fun executeTest12() {
        executeTestWithTimeout(12) {
            val startTime = System.currentTimeMillis()
            var issuer: String? = null
            var expiry: String? = null
            var errorMsg: String? = null
            var status = TestStatus.FAIL

            withContext(Dispatchers.IO) {
                try {
                    val url = java.net.URL("https://api.chandabook.com")
                    val conn = url.openConnection() as javax.net.ssl.HttpsURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.connect()
                    val certs = conn.serverCertificates
                    if (certs.isNotEmpty()) {
                        val cert = certs[0] as X509Certificate
                        issuer = cert.issuerX500Principal.name
                        expiry = cert.notAfter.toString()
                        cert.checkValidity()
                        status = TestStatus.PASS
                    } else {
                        errorMsg = "No certificates found"
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    errorMsg = e.localizedMessage ?: "SSL Verification failed"
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            val body = if (status == TestStatus.PASS) {
                "Issuer: $issuer\nExpiry: $expiry"
            } else null

            TestResult(
                testNumber = 12,
                testName = "SSL Certificate Check (api.chandabook.com)",
                status = status,
                responseTimeMs = elapsed,
                responseBody = body,
                errorMessage = errorMsg
            )
        }
    }
}

class ConnectionDiagnosticActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                DiagnosticScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticViewModel = viewModel()
) {
    val context = LocalContext.current
    val results by viewModel.results.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Server Diagnostics",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF4C542)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Login",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F2F3A)
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                color = Color(0xFF0F2F3A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.runDiagnostics(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF4C542),
                            contentColor = Color(0xFF0F2F3A)
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF0F2F3A),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Running Diagnostics...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        } else {
                            Text("▶ Run All Tests", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val report = buildDiagnosticReport(results)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Diagnostic Report", report)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "📋 Report copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF4C542)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFF4C542)),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("📋 Copy Full Report", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        },
        containerColor = Color(0xFF0B1F27) // Standalone screen background
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Summary Card at the Top if tested
            val testsRun = results.count { it.status != TestStatus.PENDING }
            if (testsRun > 0) {
                val passes = results.count { it.status == TestStatus.PASS }
                item {
                    val diagnosis = computeDiagnosis(results)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF163D4D)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF1E5368)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Result Summary",
                                color = Color(0xFFF4C542),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "$passes / 12 tests passed",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color(0xFF1E5368))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Diagnosis:",
                                color = Color(0xFFF4C542),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                diagnosis,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            items(results) { test ->
                DiagnosticCard(test)
            }
        }
    }
}

@Composable
fun DiagnosticCard(test: TestResult) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF112D3A)),
        border = BorderStroke(1.dp, Color(0xFF1E5368)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!test.responseBody.isNullOrEmpty() || !test.errorMessage.isNullOrEmpty()) {
                    isExpanded = !isExpanded
                }
            }
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when (test.status) {
                            TestStatus.PENDING -> "⏳"
                            TestStatus.RUNNING -> "🔄"
                            TestStatus.PASS -> "✅"
                            TestStatus.FAIL -> "❌"
                        },
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Text(
                        text = "${test.testNumber}. ${test.testName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    test.responseTimeMs?.let {
                        Text(
                            text = "${it}ms",
                            fontSize = 11.sp,
                            color = Color(0xFF5A7685),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    test.httpCode?.let {
                        val color = if (it in 200..299) Color(0xFF2E7D32) else Color(0xFFC62828)
                        Box(
                            modifier = Modifier
                                .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HTTP $it",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                }
            }

            // Collapse Info hint
            val hasDetails = !test.responseBody.isNullOrEmpty() || !test.errorMessage.isNullOrEmpty()
            if (hasDetails && !isExpanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to view responses",
                    fontSize = 10.sp,
                    color = Color(0xFF5A7685),
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // Collapsible Details Section
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    if (!test.errorMessage.isNullOrEmpty()) {
                        Text(
                            text = "Error:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = Color(0xFFEF5350)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF261012), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = test.errorMessage,
                                fontSize = 11.sp,
                                color = Color(0xFFEF5350),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (!test.responseBody.isNullOrEmpty()) {
                        if (!test.errorMessage.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            text = "Response Body:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = Color(0xFFF4C542)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B1418), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = test.responseBody,
                                fontSize = 11.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 10
                            )
                        }
                    }
                }
            }
        }
    }
}

fun computeDiagnosis(results: List<TestResult>): String {
    val t2 = results.find { it.testNumber == 2 }
    val t3 = results.find { it.testNumber == 3 }
    val t4 = results.find { it.testNumber == 4 }
    val t6 = results.find { it.testNumber == 6 }
    val t7 = results.find { it.testNumber == 7 }
    val t8 = results.find { it.testNumber == 8 }

    if (t2?.status == TestStatus.FAIL && t3?.status == TestStatus.FAIL) {
        return "❌ Server is completely unreachable.\n\nSSH into 129.159.23.12 and run:\npm2 list\nCheck if chandabook app is running."
    }

    if (t2?.status == TestStatus.PASS && t4?.httpCode == 404) {
        return "⚠️ Website loads but /api/ route not found.\n\nThe Node.js server may not be running.\n\nSSH and run:\npm2 logs chandabook --lines 50"
    }

    if (t6?.httpCode == 404 && t7?.httpCode == 404 && t8?.httpCode == 404) {
        return "❌ Auth routes are not registered.\n\nserver.js is missing route registration.\nCheck that app.use('/api/auth', authRouter)\nis present and server.js is not commented out."
    }

    if (t6?.httpCode == 404 && t7?.httpCode == 401) {
        return "⚠️ Google auth works but Email OTP route is missing.\n\nAdd POST /api/auth/register/email route to your Node.js server."
    }

    if (t8?.httpCode == 404) {
        return "❌ Login email route missing — this is why OTP login shows HTTP 404 error.\n\nAdd POST /api/auth/login/email to server."
    }

    val anyFail = results.any { it.status == TestStatus.FAIL }
    if (!anyFail) {
        return "✅ All systems connected! If login still fails check Firebase console Google OAuth settings and SHA-1 fingerprint."
    }

    return "⚠️ Diagnostic incomplete or specific endpoints failed. Please check error codes and response bodies above."
}

fun buildDiagnosticReport(results: List<TestResult>): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val timestamp = sdf.format(Date())
    val deviceModel = Build.MODEL
    val androidVersion = Build.VERSION.RELEASE

    val builder = StringBuilder()
    builder.append("ChandaBook Diagnostic Report\n")
    builder.append("Generated: $timestamp\n")
    builder.append("Device: $deviceModel Android $androidVersion\n")
    builder.append("---\n")

    results.forEach { test ->
        val statusIcon = when (test.status) {
            TestStatus.PENDING -> "PENDING"
            TestStatus.RUNNING -> "RUNNING"
            TestStatus.PASS -> "PASS"
            TestStatus.FAIL -> "FAIL"
        }
        val codeAndDuration = if (test.status != TestStatus.PENDING) {
            val codePart = test.httpCode?.let { "Code $it" } ?: ""
            val timePart = test.responseTimeMs?.let { "${it}ms" } ?: ""
            val parts = listOf(codePart, timePart).filter { it.isNotEmpty() }
            if (parts.isNotEmpty()) " (${parts.joinToString(", ")})" else ""
        } else ""

        builder.append("TEST ${test.testNumber} - ${test.testName}: $statusIcon$codeAndDuration\n")
        if (!test.errorMessage.isNullOrEmpty()) {
            builder.append("  Error: ${test.errorMessage}\n")
        }
        if (!test.responseBody.isNullOrEmpty()) {
            builder.append("  Response: ${test.responseBody?.trim()}\n")
        }
    }
    builder.append("---\n")
    builder.append("DIAGNOSIS:\n")
    builder.append(computeDiagnosis(results))
    return builder.toString()
}
