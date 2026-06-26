package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AnimeDatabase
import com.example.data.local.AnimeEntity
import com.example.data.model.AnimeRecommendation
import com.example.data.model.JikanAnime
import com.example.data.repository.AnimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.Exception

data class EnrichedRecommendation(
    val recommendation: AnimeRecommendation,
    val jikanAnime: JikanAnime? = null,
    val isLoadingJikan: Boolean = true
)

class AnimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AnimeRepository

    init {
        val database = AnimeDatabase.getDatabase(application)
        repository = AnimeRepository(database.animeDao())
    }

    // --- State Observables ---

    // Discover & Search
    private val _topAnime = MutableStateFlow<List<JikanAnime>>(emptyList())
    val topAnime: StateFlow<List<JikanAnime>> = _topAnime.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<JikanAnime>>(emptyList())
    val searchResults: StateFlow<List<JikanAnime>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Watchlist
    val watchlist: StateFlow<List<AnimeEntity>> = repository.watchlist
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // AI Recommender
    private val _aiPrompt = MutableStateFlow("")
    val aiPrompt: StateFlow<String> = _aiPrompt.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiRecommendations = MutableStateFlow<List<EnrichedRecommendation>>(emptyList())
    val aiRecommendations: StateFlow<List<EnrichedRecommendation>> = _aiRecommendations.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    // Search debounce job
    private var searchJob: Job? = null

    init {
        loadTopAnime()
    }

    fun loadTopAnime() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val animeList = repository.getTopAnime()
                _topAnime.value = animeList
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load top anime: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _isSearching.value = false
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(600) // Debounce network calls
            try {
                val results = repository.searchAnime(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    // --- Watchlist Interactions ---

    fun toggleWatchlist(anime: JikanAnime) {
        viewModelScope.launch {
            val alreadySaved = repository.isSaved(anime.malId)
            if (alreadySaved) {
                repository.removeFromWatchlist(anime.malId)
            } else {
                val entity = AnimeEntity(
                    malId = anime.malId,
                    title = anime.title,
                    imageUrl = anime.images?.jpg?.largeImageUrl ?: anime.images?.jpg?.imageUrl ?: "",
                    synopsis = anime.synopsis ?: "No synopsis available.",
                    score = anime.score ?: 0.0,
                    episodes = anime.episodes ?: 0,
                    genres = anime.genres?.joinToString(", ") { it.name } ?: ""
                )
                repository.addToWatchlist(entity)
            }
        }
    }

    fun toggleWatchlist(entity: AnimeEntity) {
        viewModelScope.launch {
            repository.removeFromWatchlist(entity.malId)
        }
    }

    fun isSavedFlow(malId: Int): Flow<Boolean> {
        return repository.isSavedFlow(malId)
    }

    // --- AI Recommendations ---

    fun updateAiPrompt(prompt: String) {
        _aiPrompt.value = prompt
    }

    fun generateRecommendations() {
        val prompt = _aiPrompt.value
        if (prompt.isBlank()) return

        viewModelScope.launch {
            _isAiLoading.value = true
            _aiError.value = null
            _aiRecommendations.value = emptyList()

            try {
                val baseRecs = repository.getAiRecommendations(prompt)
                
                // Set initial state with empty Jikan details
                val enrichedList = baseRecs.map { EnrichedRecommendation(recommendation = it) }.toMutableList()
                _aiRecommendations.value = enrichedList

                // Fetch Jikan details for each asynchronously to load posters and ratings
                baseRecs.forEachIndexed { index, recommendation ->
                    launch(Dispatchers.IO) {
                        try {
                            val results = repository.searchAnime(recommendation.title)
                            val match = results.firstOrNull() ?: results.firstOrNull { 
                                it.title.contains(recommendation.title, ignoreCase = true) 
                            }
                            
                            synchronized(enrichedList) {
                                val current = enrichedList[index]
                                enrichedList[index] = current.copy(
                                    jikanAnime = match,
                                    isLoadingJikan = false
                                )
                                // Force state update by re-assigning list
                                _aiRecommendations.value = enrichedList.toList()
                            }
                        } catch (e: Exception) {
                            synchronized(enrichedList) {
                                val current = enrichedList[index]
                                enrichedList[index] = current.copy(isLoadingJikan = false)
                                _aiRecommendations.value = enrichedList.toList()
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                _aiError.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isAiLoading.value = false
            }
        }
    }
}
