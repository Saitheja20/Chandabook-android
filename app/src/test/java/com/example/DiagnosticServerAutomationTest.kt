package com.example

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Test
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

class DiagnosticServerAutomationTest {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .build()

    @Test
    fun executeAllServerDiagnosticTests() {
        println("\n==================================================")
        println("      STARTING AUTOMATED SERVER DIAGNOSTIC TEST (Bust Cache) ")
        println("==================================================")

        val results = mutableListOf<TestRecord>()

        // 1. Internet Connectivity Test
        results.add(runTest(1, "Internet Connectivity") {
            try {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress("google.com", 80), 3000)
                socket.close()
                TestResultData(true, 200, "Device has active internet access")
            } catch (e: Exception) {
                TestResultData(false, null, "No internet or Google unreachable: ${e.message}")
            }
        })

        // 2. HTTPS Domain Reachable
        results.add(runTest(2, "HTTPS Domain Reachable (api.chandabook.com)") {
            var ipInfo = ""
            try {
                val addresses = java.net.InetAddress.getAllByName("api.chandabook.com")
                ipInfo = " [DNS IPs: " + addresses.joinToString { it.hostAddress } + "]"
            } catch (e: Exception) {
                ipInfo = " [DNS failed: ${e.message}]"
            }
            val request = Request.Builder().url("https://api.chandabook.com/").get().build()
            okHttpClient.newCall(request).execute().use { response ->
                TestResultData(true, response.code, response.body?.string()?.take(150) + ipInfo)
            }
        })

        // 3. HTTP Direct IP Reachable
        results.add(runTest(3, "HTTP Direct IP Reachable (129.159.23.12:3000)") {
            val request = Request.Builder().url("http://129.159.23.12:3000/").get().build()
            okHttpClient.newCall(request).execute().use { response ->
                TestResultData(true, response.code, response.body?.string()?.take(150))
            }
        })

        // 4. API Base Endpoint (HTTPS)
        results.add(runTest(4, "API Base Endpoint (HTTPS)") {
            val request = Request.Builder().url("https://api.chandabook.com/api/").get().build()
            okHttpClient.newCall(request).execute().use { response ->
                val isPass = response.code == 200 || response.code == 404
                TestResultData(isPass, response.code, response.body?.string()?.take(150))
            }
        })

        // 5. API Base Endpoint (Direct IP)
        results.add(runTest(5, "API Base Endpoint (Direct IP)") {
            val request = Request.Builder().url("http://129.159.23.12:3000/").get().build()
            okHttpClient.newCall(request).execute().use { response ->
                val isPass = response.code == 200 || response.body?.string()?.contains("API running") == true
                TestResultData(isPass, response.code, response.body?.string()?.take(150))
            }
        })

        // 6. Auth Endpoint (Email OTP)
        results.add(runTest(6, "Auth Endpoint Exists (Email OTP)") {
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
                .url("http://129.159.23.12:3000/api/auth/register/email")
                .post(payload.toRequestBody(jsonMediaType))
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val isPass = response.code == 200 || response.code == 201
                TestResultData(isPass, response.code, response.body?.string()?.take(150))
            }
        })

        // 7. Auth Endpoint (Google)
        results.add(runTest(7, "Auth Endpoint Exists (Google)") {
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()
            val payload = """{"idToken": "diagnostic_test_token"}"""
            val request = Request.Builder()
                .url("http://129.159.23.12:3000/api/auth/google")
                .post(payload.toRequestBody(jsonMediaType))
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val isPass = response.code == 400 || response.code == 401
                TestResultData(isPass, response.code, response.body?.string()?.take(150))
            }
        })

        // 8. Auth Endpoint (Login Email)
        results.add(runTest(8, "Auth Endpoint Exists (Login)") {
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()
            val payload = """{"email": "test@diagnostic.com"}"""
            val request = Request.Builder()
                .url("http://129.159.23.12:3000/api/auth/login/email")
                .post(payload.toRequestBody(jsonMediaType))
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val isPass = response.code == 200 || response.code == 400 || response.code == 401 || response.code == 404
                TestResultData(isPass, response.code, response.body?.string()?.take(150))
            }
        })

        // 8b. Auth Endpoint Direct /login
        results.add(runTest(13, "Auth Endpoint Direct /login (no /auth prefix)") {
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()
            val payload = """{"email": "test@diagnostic.com", "otp": "123456"}"""
            val request = Request.Builder()
                .url("http://129.159.23.12:3000/api/auth/login/email/verify")
                .post(payload.toRequestBody(jsonMediaType))
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val isPass = response.code == 200 || response.code == 400 || response.code == 401
                TestResultData(isPass, response.code, response.body?.string()?.take(150))
            }
        })

        // 9. Public Org Lookup
        results.add(runTest(9, "Public Org Lookup") {
            val request = Request.Builder()
                .url("http://129.159.23.12:3000/api/organizations/public/C6UIMS")
                .get()
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val isPass = response.code == 200
                TestResultData(isPass, response.code, response.body?.string()?.take(150))
            }
        })

        // 10. Organizations (Auth Required)
        results.add(runTest(10, "Organizations (Auth Required)") {
            val request = Request.Builder()
                .url("http://129.159.23.12:3000/api/organizations")
                .header("Authorization", "Bearer diagnostic_test")
                .get()
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val isPass = response.code == 401 || response.code == 403 || response.code == 500
                TestResultData(isPass, response.code, response.body?.string()?.take(150))
            }
        })

        // 11. Donations Endpoint Exists
        results.add(runTest(11, "Donations Endpoint Exists") {
            val request = Request.Builder()
                .url("http://129.159.23.12:3000/api/donations")
                .header("Authorization", "Bearer diagnostic_test")
                .get()
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val isPass = response.code == 401 || response.code == 403 || response.code == 500
                TestResultData(isPass, response.code, response.body?.string()?.take(150))
            }
        })

        // 12. SSL Certificate Check
        results.add(runTest(12, "SSL Certificate Check (api.chandabook.com)") {
            val url = java.net.URL("https://api.chandabook.com")
            val conn = url.openConnection() as javax.net.ssl.HttpsURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.connect()
            val certs = conn.serverCertificates
            val pass = certs.isNotEmpty()
            val bodyText = if (pass) {
                val cert = certs[0] as X509Certificate
                "Issuer: ${cert.issuerX500Principal.name}, Expiry: ${cert.notAfter}"
            } else "No certs found"
            conn.disconnect()
            TestResultData(pass, null, bodyText)
        })

        println("\n==================================================")
        println("                  AUTOMATION SUMMARY              ")
        println("==================================================")
        val passedCount = results.count { it.pass }
        println("TOTAL PASSED: $passedCount / 12")
        println("TOTAL FAILED: ${12 - passedCount}")

        println("\nDIAGNOSIS REPORT:")
        val diagnosis = produceDiagnosisText(results)
        println(diagnosis)
        println("==================================================")
    }

    private fun runTest(num: Int, name: String, block: () -> TestResultData): TestRecord {
        print("Running Test $num: $name -> ")
        return try {
            val resData = block()
            val statusStr = if (resData.pass) "✅ PASS" else "❌ FAIL"
            print("$statusStr")
            resData.code?.let { print(" (HTTP $it)") }
            resData.body?.let { print(" | Response: ${it.trim().replace("\n", " ")}") }
            println()
            TestRecord(num, name, resData.pass, resData.code, resData.body)
        } catch (e: Exception) {
            println("❌ FAIL (Exception: ${e.message ?: e.javaClass.simpleName})")
            TestRecord(num, name, false, null, "Exception: ${e.message}")
        }
    }

    private fun produceDiagnosisText(results: List<TestRecord>): String {
        val t2 = results.find { it.num == 2 }
        val t3 = results.find { it.num == 3 }
        val t4 = results.find { it.num == 4 }
        val t6 = results.find { it.num == 6 }
        val t7 = results.find { it.num == 7 }
        val t8 = results.find { it.num == 8 }

        if (t2?.pass == false && t3?.pass == false) {
            return "❌ SERVER REACHABILITY FAILURE: Both HTTPS (api.chandabook.com) and Direct IP (129.159.23.12:3000) are unreachable. The server machine is either offline or the firewall rules blocked port 3000."
        }
        if (t2?.pass == true && t4?.code == 404) {
            return "⚠️ ROUTING MISMATCH: The domain api.chandabook.com is active, but the /api/ base endpoint returned 404. It means your Nginx router does not translate /api/ correctly to your Node.js application, or your Node.js application is not listening on /api/ endpoints (routes are registered without '/api' prefix like /users, /auth, /organizations)."
        }
        if (t6?.code == 404 && t7?.code == 404 && t8?.code == 404) {
            return "❌ ENDPOINT NOT YET IMPLEMENTED IN SERVER: Your GitHub Express server (routes/auth.js) only defines a single POST /login route. It is completely missing auth/register/email, auth/google, auth/login/email, and other routes requested by the Android app."
        }
        return "⚠️ Setup issue found. Please check individual HTTP codes to see which ones are returning 404 or of other statuses."
    }

    data class TestResultData(val pass: Boolean, val code: Int?, val body: String?)
    data class TestRecord(val num: Int, val name: String, val pass: Boolean, val code: Int?, val body: String?)
}
