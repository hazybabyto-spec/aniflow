package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.RetrofitClient
import com.example.data.local.AnimeDao
import com.example.data.local.AnimeEntity
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.lang.Exception

class AnimeRepository(private val animeDao: AnimeDao) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // --- Local Watchlist Methods ---
    val watchlist: Flow<List<AnimeEntity>> = animeDao.getWatchlist()

    suspend fun addToWatchlist(anime: AnimeEntity) = withContext(Dispatchers.IO) {
        animeDao.insert(anime)
    }

    suspend fun removeFromWatchlist(malId: Int) = withContext(Dispatchers.IO) {
        animeDao.deleteById(malId)
    }

    fun isSavedFlow(malId: Int): Flow<Boolean> {
        return animeDao.isSavedFlow(malId)
    }

    suspend fun isSaved(malId: Int): Boolean = withContext(Dispatchers.IO) {
        animeDao.isSaved(malId)
    }

    // --- Jikan API Methods ---
    suspend fun getTopAnime(): List<JikanAnime> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.jikanService.getTopAnime()
            response.data
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchAnime(query: String): List<JikanAnime> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) return@withContext emptyList()
            val response = RetrofitClient.jikanService.searchAnime(query)
            response.data
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAnimeDetails(malId: Int): JikanAnime? = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.jikanService.getAnimeDetails(malId)
            response.data
        } catch (e: Exception) {
            null
        }
    }

    // --- Gemini Recommender Method ---
    suspend fun getAiRecommendations(userPrompt: String): List<AnimeRecommendation> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API Key is not configured in Secrets.")
        }

        val systemInstruction = """
            You are AnimeMind, an expert AI Anime recommendation assistant. 
            Your task is to recommend exactly 5 anime series that perfectly match the user's requested mood, genre preferences, or prompt.
            You MUST return your response in a structured JSON format. 
            The JSON MUST have a single top-level key "recommendations" containing a JSON array of recommendation objects.
            Each object in the array must contain:
            1. "title" (String): The precise title of the anime.
            2. "reason" (String): A compelling, personalized explanation (1-2 sentences) of why this anime matches the user's prompt.
            3. "mood_match" (Int): A match percentage between 50 and 100 based on the prompt's alignment.
            4. "genres" (List of Strings): 2-4 genre or theme tags for the anime.

            Do NOT include any markdown block ticks, code comments, or text outside of the JSON block. Output raw, clean JSON text directly.
        """.trimIndent()

        val fullPrompt = "User Request: \"$userPrompt\"\nGenerate recommendations matching this request."

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = "$systemInstruction\n\n$fullPrompt")
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.7
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from AI")

            val cleanedJson = cleanJson(responseText)
            val adapter = moshi.adapter(GeminiRecommendationsWrapper::class.java)
            val wrapper = adapter.fromJson(cleanedJson)
            wrapper?.recommendations ?: emptyList()
        } catch (e: Exception) {
            throw e
        }
    }

    private fun cleanJson(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("```").substringBeforeLast("```").trim()
        }
        return cleaned
    }
}
