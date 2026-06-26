package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- Jikan API Models ---

@JsonClass(generateAdapter = true)
data class JikanResponse(
    val data: List<JikanAnime>
)

@JsonClass(generateAdapter = true)
data class JikanAnimeResponse(
    val data: JikanAnime
)

@JsonClass(generateAdapter = true)
data class JikanAnime(
    @Json(name = "mal_id") val malId: Int,
    val title: String,
    val synopsis: String? = null,
    val images: JikanImages? = null,
    val score: Double? = null,
    val episodes: Int? = null,
    val genres: List<JikanGenre>? = null,
    val status: String? = null,
    val rating: String? = null,
    val type: String? = null
)

@JsonClass(generateAdapter = true)
data class JikanImages(
    val jpg: JikanImageSource? = null
)

@JsonClass(generateAdapter = true)
data class JikanImageSource(
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "large_image_url") val largeImageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class JikanGenre(
    val name: String
)

// --- Gemini API Models ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

// --- Domain & AI Recommendation UI Models ---

@JsonClass(generateAdapter = true)
data class AnimeRecommendation(
    val title: String,
    val reason: String,
    @Json(name = "mood_match") val moodMatch: Int,
    val genres: List<String>
)

@JsonClass(generateAdapter = true)
data class GeminiRecommendationsWrapper(
    val recommendations: List<AnimeRecommendation>
)
