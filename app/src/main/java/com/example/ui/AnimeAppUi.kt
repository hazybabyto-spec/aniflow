package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.example.R
import com.example.data.local.AnimeEntity
import com.example.data.model.AnimeRecommendation
import com.example.data.model.JikanAnime
import com.example.ui.theme.*

enum class AnimeTab {
    DISCOVER, WATCHLIST, AI_RECOMMENDER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeAppContent(
    viewModel: AnimeViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AnimeTab.DISCOVER) }
    var detailAnime by remember { mutableStateOf<JikanAnime?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            AnimeBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content based on active tab
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(300),
                label = "ScreenTransition"
            ) { tab ->
                when (tab) {
                    AnimeTab.DISCOVER -> {
                        DiscoverScreen(
                            viewModel = viewModel,
                            onAnimeClick = { anime ->
                                detailAnime = anime
                                showBottomSheet = true
                            }
                        )
                    }
                    AnimeTab.WATCHLIST -> {
                        WatchlistScreen(
                            watchlist = watchlist,
                            onRemoveClick = { viewModel.toggleWatchlist(it) },
                            onAnimeClick = { entity ->
                                // Convert to JikanAnime to display in details sheet
                                val jAnime = JikanAnime(
                                    malId = entity.malId,
                                    title = entity.title,
                                    synopsis = entity.synopsis,
                                    images = com.example.data.model.JikanImages(
                                        jpg = com.example.data.model.JikanImageSource(entity.imageUrl)
                                    ),
                                    score = entity.score,
                                    episodes = entity.episodes,
                                    genres = entity.genres.split(", ").filter { it.isNotBlank() }.map { com.example.data.model.JikanGenre(it) }
                                )
                                detailAnime = jAnime
                                showBottomSheet = true
                            },
                            onBrowseClick = { selectedTab = AnimeTab.DISCOVER }
                        )
                    }
                    AnimeTab.AI_RECOMMENDER -> {
                        AiRecommenderScreen(
                            viewModel = viewModel,
                            onAnimeClick = { anime ->
                                detailAnime = anime
                                showBottomSheet = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet
    if (showBottomSheet && detailAnime != null) {
        AnimeDetailSheet(
            anime = detailAnime!!,
            viewModel = viewModel,
            onDismiss = {
                showBottomSheet = false
                detailAnime = null
            }
        )
    }
}

// --- Custom Bottom Navigation Bar ---

@Composable
fun AnimeBottomNavigation(
    selectedTab: AnimeTab,
    onTabSelected: (AnimeTab) -> Unit
) {
    Surface(
        color = CardSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color.White.copy(0.05f))
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                AnimeTab.DISCOVER to "Discover" to Icons.Default.Explore to Icons.Outlined.Explore,
                AnimeTab.WATCHLIST to "Watchlist" to Icons.Default.Favorite to Icons.Outlined.FavoriteBorder,
                AnimeTab.AI_RECOMMENDER to "AI Companion" to Icons.Default.AutoAwesome to Icons.Outlined.AutoAwesome
            )

            tabs.forEach { (firstPart, inactiveIcon) ->
                val (secondPart, activeIcon) = firstPart
                val (tab, label) = secondPart
                val isSelected = selectedTab == tab
                val iconColor = if (isSelected) NeonPink else DarkGrayText
                val textColor = if (isSelected) Color.White else DarkGrayText

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 8.dp)
                        .testTag("nav_tab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) activeIcon else inactiveIcon,
                        contentDescription = label,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

// --- ANIME GRID CARD (RESPONSIVE WITH HOVER & FOCUS EFFECTS) ---

@Composable
fun AnimeGridCard(
    anime: JikanAnime,
    viewModel: AnimeViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSaved by viewModel.isSavedFlow(anime.malId).collectAsStateWithLifecycle(initialValue = false)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Interactive Hover/Focus Scale & Glow Animations
    val scale by animateFloatAsState(
        targetValue = if (isHovered || isFocused) 1.05f else 1.00f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isHovered || isFocused) NeonCyan else Color.White.copy(0.08f),
        animationSpec = tween(200),
        label = "borderColor"
    )

    val shadowElevation by animateDpAsState(
        targetValue = if (isHovered || isFocused) 12.dp else 2.dp,
        animationSpec = tween(200),
        label = "elevation"
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .shadow(elevation = shadowElevation, shape = RoundedCornerShape(16.dp), spotColor = NeonCyan)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f) // Standard movie poster ratio
        ) {
            // Poster Image with customized loading/error placeholders
            SubcomposeAsyncImage(
                model = anime.images?.jpg?.largeImageUrl ?: anime.images?.jpg?.imageUrl,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    // Shimmery gradient cyberpunk placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CardSurface, GlassWhite, CardSurface)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NeonPink,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    // Custom fallback placeholder if no image or error occurs
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(MysticPurple, DarkBackground)
                                )
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ANIME",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "NO IMAGE",
                                color = NeonPink,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                }
            )

            // Star Rating Badge (Top End)
            if (anime.score != null && anime.score > 0.0) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Color.Black.copy(0.75f), RoundedCornerShape(6.dp))
                        .border(0.5.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color.Yellow,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = String.format("%.1f", anime.score),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Watchlist Toggle Heart Icon (Top Start)
            IconButton(
                onClick = { viewModel.toggleWatchlist(anime) },
                modifier = Modifier
                    .padding(8.dp)
                    .size(32.dp)
                    .background(Color.Black.copy(0.6f), CircleShape)
                    .border(0.5.dp, if (isSaved) NeonPink else Color.Transparent, CircleShape)
                    .align(Alignment.TopStart)
                    .testTag("watchlist_toggle_${anime.malId}")
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isSaved) "Remove Watchlist" else "Save Watchlist",
                    tint = if (isSaved) NeonPink else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Text Info & Banner overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(0.9f))
                        )
                    )
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = anime.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (anime.episodes != null) "${anime.episodes} eps" else "Movie",
                            color = DarkGrayText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Highlight tag for the primary genre
                        anime.genres?.firstOrNull()?.name?.let { primaryGenre ->
                            Box(
                                modifier = Modifier
                                    .background(GlassWhite, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = primaryGenre,
                                    color = NeonCyan,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- DISCOVER SCREEN ---

@Composable
fun DiscoverScreen(
    viewModel: AnimeViewModel,
    onAnimeClick: (JikanAnime) -> Unit
) {
    val topAnime by viewModel.topAnime.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 135.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Header Banner (Spans full width)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.pxToDp()) // Custom high-contrast banner height
            ) {
                // Background image banner
                AsyncImage(
                    model = R.drawable.anime_hero_banner,
                    contentDescription = "Futuristic Neon Tokyo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    DarkBackground.copy(alpha = 0.5f),
                                    DarkBackground
                                )
                            )
                        )
                )
                // App Logo Title overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonPink)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ANIME EXPLORER",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Explore top hits & AI custom recommendations",
                        fontSize = 12.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Search pill (Spans full width)
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search any anime series...", color = DarkGrayText) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NeonCyan
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = DarkGrayText
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassWhite,
                    focusedContainerColor = CardSurface.copy(alpha = 0.8f),
                    unfocusedContainerColor = CardSurface.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_input")
            )
        }

        // Search Results state or Popular Anime state
        if (searchQuery.isNotBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Search Results",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (isSearching) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
            } else if (searchResults.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = DarkGrayText,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No anime matches found",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                items(searchResults) { anime ->
                    AnimeGridCard(
                        anime = anime,
                        viewModel = viewModel,
                        onClick = { onAnimeClick(anime) },
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .testTag("anime_grid_card_${anime.malId}")
                    )
                }
            }
        } else {
            // Default top hits popular list
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Trending Releases",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPink,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonPink)
                    }
                }
            } else if (errorMessage != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = errorMessage!!, color = Color.Red, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.loadTopAnime() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                items(topAnime) { anime ->
                    AnimeGridCard(
                        anime = anime,
                        viewModel = viewModel,
                        onClick = { onAnimeClick(anime) },
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .testTag("anime_grid_card_${anime.malId}")
                    )
                }
            }
        }
    }
}

// Helper to convert px to dp for our banner (estimating standard 16:9 banner)
@Composable
fun pxToDp(): Int {
    return 240
}

@Composable
fun Int.pxToDp() = this.dp

// --- WATCHLIST SCREEN ---

@Composable
fun WatchlistScreen(
    watchlist: List<AnimeEntity>,
    onRemoveClick: (AnimeEntity) -> Unit,
    onAnimeClick: (AnimeEntity) -> Unit,
    onBrowseClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Watchlist Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface)
                .padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Column {
                Text(
                    text = "My Watchlist",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "${watchlist.size} series tracked",
                    fontSize = 13.sp,
                    color = NeonPink,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Watchlist contents
        if (watchlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = DarkGrayText,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your Watchlist is Empty",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Find anime to save using Discover or ask our AI Neural Companion!",
                        fontSize = 13.sp,
                        color = DarkGrayText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBrowseClick,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Go Discover", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(watchlist) { entity ->
                    WatchlistCard(
                        entity = entity,
                        onRemoveClick = { onRemoveClick(entity) },
                        onAnimeClick = { onAnimeClick(entity) }
                    )
                }
            }
        }
    }
}

// --- AI RECOMMENDER / COMPANION SCREEN ---

@Composable
fun AiRecommenderScreen(
    viewModel: AnimeViewModel,
    onAnimeClick: (JikanAnime) -> Unit
) {
    val aiPrompt by viewModel.aiPrompt.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiRecommendations by viewModel.aiRecommendations.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val promptSuggestions = listOf(
        "A cyberpunk thriller like Psycho-Pass with incredible art",
        "Something wholesomely cozy and peaceful for a rainy evening",
        "A dark psychological thriller featuring intense mind games",
        "Mind-bending action anime with unique magic and time travel"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // AI Companion header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MysticPurple.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "AnimeMind AI",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Consult the neural oracle to discover your next obsession",
                        fontSize = 12.sp,
                        color = DarkGrayText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Prompt input area
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = aiPrompt,
                    onValueChange = { viewModel.updateAiPrompt(it) },
                    placeholder = {
                        Text(
                            "Describe what kind of anime you are in the mood for...",
                            color = DarkGrayText,
                            fontSize = 14.sp
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPink,
                        unfocusedBorderColor = GlassWhite,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_prompt_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        viewModel.generateRecommendations()
                    },
                    enabled = aiPrompt.isNotBlank() && !isAiLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPink,
                        disabledContainerColor = GlassWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("ai_generate_button")
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reading neural pathways...")
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compute Recommendations", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Suggestion Chips
        if (aiRecommendations.isEmpty() && !isAiLoading) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Need Inspiration?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    promptSuggestions.forEach { suggestion ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.updateAiPrompt(suggestion)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = suggestion,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Error message
        if (aiError != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .border(1.dp, Color.Red, RoundedCornerShape(12.dp))
                        .background(Color.Red.copy(0.1f))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = aiError!!,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Recommendations List
        if (aiRecommendations.isNotEmpty() || isAiLoading) {
            item {
                Text(
                    text = "Neural Matches",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            items(aiRecommendations) { rec ->
                AiRecommendationItem(
                    enriched = rec,
                    viewModel = viewModel,
                    onClick = {
                        rec.jikanAnime?.let { onAnimeClick(it) }
                    }
                )
            }
        }
    }
}

// --- REUSABLE ANIME CARD FOR WATCHLIST (GRID) ---

@Composable
fun WatchlistCard(
    entity: AnimeEntity,
    onRemoveClick: () -> Unit,
    onAnimeClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAnimeClick() }
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            AsyncImage(
                model = entity.imageUrl,
                contentDescription = entity.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Rating Badge overlay
            if (entity.score > 0) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Color.Black.copy(0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.Yellow,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f", entity.score),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Remove Watchlist Overlay Circular Button
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(32.dp)
                    .background(Color.Black.copy(0.7f), CircleShape)
                    .clickable { onRemoveClick() }
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from Watchlist",
                    tint = NeonPink,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Title & Info overlay inside card bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(0.8f))
                        )
                    )
                    .padding(8.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Column {
                    Text(
                        text = entity.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (entity.episodes > 0) "${entity.episodes} eps" else "Movie/OVA",
                        color = DarkGrayText,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// --- DISCOVER LIST ITEM COMPONENT ---

@Composable
fun AnimeListItem(
    anime: JikanAnime,
    viewModel: AnimeViewModel,
    onClick: () -> Unit
) {
    val isSaved by viewModel.isSavedFlow(anime.malId).collectAsStateWithLifecycle(initialValue = false)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(0.3f))
            ) {
                AsyncImage(
                    model = anime.images?.jpg?.imageUrl,
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = anime.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Genres row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val genreTags = anime.genres?.take(2)?.map { it.name } ?: emptyList()
                    genreTags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(GlassWhite, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = tag, color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Score & Episodes
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (anime.score != null) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color.Yellow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f", anime.score),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Text(
                        text = if (anime.episodes != null) "${anime.episodes} Ep" else "Unknown Ep",
                        color = DarkGrayText,
                        fontSize = 12.sp
                    )
                }
            }

            // Heart Watchlist Toggle Icon
            IconButton(
                onClick = { viewModel.toggleWatchlist(anime) },
                modifier = Modifier.testTag("watchlist_toggle_${anime.malId}")
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isSaved) "Remove Watchlist" else "Save Watchlist",
                    tint = if (isSaved) NeonPink else DarkGrayText
                )
            }
        }
    }
}

// --- AI RECOMMENDATION CARD COMPONENT ---

@Composable
fun AiRecommendationItem(
    enriched: EnrichedRecommendation,
    viewModel: AnimeViewModel,
    onClick: () -> Unit
) {
    val rec = enriched.recommendation
    val anime = enriched.jikanAnime
    val isSaved = if (anime != null) {
        viewModel.isSavedFlow(anime.malId).collectAsStateWithLifecycle(initialValue = false).value
    } else false

    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(enabled = anime != null) { onClick() }
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Recommendation header (Confidence & Title)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Confidence Match Badge
                Box(
                    modifier = Modifier
                        .background(NeonPink.copy(0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, NeonPink, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${rec.moodMatch}% Match",
                        color = NeonPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = rec.title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (anime != null) {
                    IconButton(
                        onClick = { viewModel.toggleWatchlist(anime) }
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Watchlist toggle",
                            tint = if (isSaved) NeonPink else DarkGrayText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI's Explanation text block
            Text(
                text = rec.reason,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Jikan API Enriched Poster Info (Loads dynamically!)
            if (enriched.isLoadingJikan) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    CircularProgressIndicator(
                        color = NeonCyan,
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 1.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enriching with MyAnimeList data...",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (anime != null) {
                Divider(color = Color.White.copy(0.05f), modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minified poster thumbnail
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 60.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(0.3f))
                    ) {
                        AsyncImage(
                            model = anime.images?.jpg?.imageUrl,
                            contentDescription = anime.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (anime.score != null) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.Yellow,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = String.format("%.1f", anime.score),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = if (anime.episodes != null) "${anime.episodes} Ep" else "Unknown Ep",
                                color = DarkGrayText,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Tags row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rec.genres.take(3).forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .background(GlassWhite, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = genre, color = NeonCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- DETAIL MODAL BOTTOM SHEET COMPONENT ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailSheet(
    anime: JikanAnime,
    viewModel: AnimeViewModel,
    onDismiss: () -> Unit
) {
    val isSaved by viewModel.isSavedFlow(anime.malId).collectAsStateWithLifecycle(initialValue = false)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GlassWhite) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Larger poster
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(0.3f))
                ) {
                    AsyncImage(
                        model = anime.images?.jpg?.largeImageUrl ?: anime.images?.jpg?.imageUrl,
                        contentDescription = anime.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Metadata Details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = anime.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (anime.score != null) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color.Yellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", anime.score),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        Text(
                            text = if (anime.episodes != null) "${anime.episodes} Ep" else "Unknown Ep",
                            color = DarkGrayText,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Genres wrap panel
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        anime.genres?.map { it.name }?.forEach { name ->
                            Box(
                                modifier = Modifier
                                    .background(GlassWhite, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = name,
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Watchlist toggle button
            Button(
                onClick = { viewModel.toggleWatchlist(anime) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaved) CardSurface else NeonPink,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(
                        width = if (isSaved) 1.dp else 0.dp,
                        color = if (isSaved) NeonPink else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaved) "Remove from Watchlist" else "Add to Watchlist",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Synopsis Scroll Area
            Text(
                text = "Synopsis",
                color = NeonPink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = CardSurface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = anime.synopsis ?: "No synopsis has been provided for this series.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// FlowRow wrapper simple implementation since standard FlowRow is part of experimental layout in some older compose libraries
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Very simple row representation as items are limited.
            // Under proper libraries, standard FlowRow handles wraps automatically.
            Row(
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.drawWithContent { drawContent() }
            ) {
                content()
            }
        }
    }
}
