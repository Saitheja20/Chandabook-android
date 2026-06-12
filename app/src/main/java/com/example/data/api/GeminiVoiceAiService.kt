package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiVoiceAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun extractExpenseFromSpeech(
        speechText: String,
        onSuccess: (title: String, amount: Double, category: String, paymentMethod: String, notes: String?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            onFailure("Gemini API key is not configured. Please add it to your Secrets panel")
            return
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val prompt = """
            You are ChandaBook AI, a smart assistant helping Indian community festival committees track and parse expenses in digital ledger apps.
            Analyze this spoken user input and extract the expense features exactly.
            Spoken Text: "$speechText"
            
            You MUST return a JSON object with strictly the following fields:
            
            1. "title": Short, descriptive category/item name in English (e.g. "Flowers for Pooja", "Sound System Hire", "Prasad Sweets"). Keep it brief (2-5 words).
            2. "amount": Numeric double value. If no amount can be extracted, default to 0.0.
            3. "category": Must be EXACTLY one of: "decoration", "pooja_items", "sound", "prasad", "printing", "transport", or "other". If not clear, default to "other".
            4. "paymentMethod": Must be EXACTLY one of: "cash", "upi", "bank_transfer", "cheque", or "online". If not clear, default to "upi".
            5. "notes": Optional text with additional info (or empty string/null).
            
            Ensure the response is STRICTLY valid JSON only, with no markdown tags, no ```json formatting, and no additional explanations.
        """.trimIndent()

        try {
            val requestJson = JSONObject()
            val contentsArray = org.json.JSONArray()
            val contentObj = JSONObject()
            val partsArray = org.json.JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // Force JSON schema configuration
            val configObj = JSONObject()
            configObj.put("responseMimeType", "application/json")
            requestJson.put("generationConfig", configObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("GeminiVoiceAiService", "Gemini call failed", e)
                    onFailure(e.localizedMessage ?: "Network connection failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string() ?: ""
                        Log.d("GeminiVoiceAiService", "Response received: $responseBody")
                        if (!response.isSuccessful) {
                            onFailure("Gemini API error (HTTP ${response.code})")
                            return
                        }

                        val jsonResponse = JSONObject(responseBody)
                        val candidates = jsonResponse.getJSONArray("candidates")
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        var text = parts.getJSONObject(0).getString("text").trim()

                        // Strip potential markdown wrappers if any
                        if (text.startsWith("```")) {
                            text = text.removePrefix("```json").removePrefix("```")
                            if (text.endsWith("```")) {
                                text = text.removeSuffix("```")
                            }
                            text = text.trim()
                        }

                        Log.d("GeminiVoiceAiService", "Extracted JSON: $text")
                        val extractedObj = JSONObject(text)
                        val title = extractedObj.optString("title", "Voice Expense")
                        val amount = extractedObj.optDouble("amount", 0.0)
                        val category = extractedObj.optString("category", "other").lowercase()
                        val paymentMethod = extractedObj.optString("paymentMethod", "upi").lowercase()
                        val notes = if (extractedObj.isNull("notes")) null else extractedObj.optString("notes")

                        val finalCategory = if (category in listOf("decoration", "pooja_items", "sound", "prasad", "printing", "transport", "other")) category else "other"
                        val finalPayment = if (paymentMethod in listOf("cash", "upi", "bank_transfer", "cheque", "online")) paymentMethod else "upi"

                        onSuccess(title, amount, finalCategory, finalPayment, notes)
                    } catch (e: Exception) {
                        Log.e("GeminiVoiceAiService", "Response parsing failed", e)
                        onFailure("We processed your speech but could not structure the expense format automatically. Try speaking like: 'Spent 1500 for flowers from cash'")
                    }
                }
            })
        } catch (e: Exception) {
            onFailure("Initialization error: ${e.localizedMessage}")
        }
    }
}
