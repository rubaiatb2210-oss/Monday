package dev.monday.data.ai

import dev.monday.domain.prompt.PromptPayload
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import dev.monday.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini Flash provider — Tier 2 AI.
 * Main reasoning engine for Monday.
 */
@Singleton
class GeminiProvider @Inject constructor(
    private val settingsRepository: SettingsRepository
) : AiProvider {
    override val name = "Gemini Flash"
    override val tier = 2

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun isAvailable(): Boolean {
        val key = getApiKey()
        return key != null && key.isNotBlank()
    }

    override suspend fun generate(prompt: PromptPayload): AiResponse {
        val apiKey = getApiKey()
            ?: throw IllegalStateException("Gemini API key not configured")

        val startTime = System.currentTimeMillis()

        val requestBody = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = "${prompt.systemPrompt}\n\n${prompt.userPrompt}"))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f,
                maxOutputTokens = 1024,
                topP = 0.95f
            )
        )

        val jsonBody = json.encodeToString(GeminiRequest.serializer(), requestBody)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: throw IllegalStateException("Empty response from Gemini")

            if (!response.isSuccessful) {
                throw IllegalStateException("Gemini API error ${response.code}: $body")
            }

            val geminiResponse = json.decodeFromString(GeminiResponse.serializer(), body)
            val text = geminiResponse.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: throw IllegalStateException("No text in Gemini response")

            val latency = System.currentTimeMillis() - startTime

            AiResponse(
                text = text,
                provider = name,
                tier = tier,
                promptId = prompt.templateId,
                promptVersion = prompt.templateVersion,
                latencyMs = latency
            )
        }
    }

    private suspend fun getApiKey(): String? {
        return settingsRepository.get("gemini_api_key")
    }
}

// Gemini API request/response models
@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Float = 0.7f,
    val maxOutputTokens: Int = 1024,
    val topP: Float = 0.95f
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null
)
