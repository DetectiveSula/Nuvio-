package com.nuvio.app.features.home.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.ScreenActivityEffect
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.stableKey
import com.nuvio.app.features.library.LibraryClock
import com.nuvio.app.features.library.LibraryRepository
import com.nuvio.app.features.library.LibrarySourceMode
import com.nuvio.app.features.library.toLibraryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_play
import nuvio.composeapp.generated.resources.home_my_list
import nuvio.composeapp.generated.resources.home_spotlight_new
import nuvio.composeapp.generated.resources.home_tab_home
import nuvio.composeapp.generated.resources.home_tab_movies
import nuvio.composeapp.generated.resources.home_tab_series
import nuvio.composeapp.generated.resources.home_tab_services
import nuvio.composeapp.generated.resources.media_movie
import nuvio.composeapp.generated.resources.media_series
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

private const val SPOTLIGHT_AUTO_ADVANCE_MS = 8_000L
private const val MS_PER_AVERAGE_YEAR = 31_556_952_000L
private val ReleaseYearRegex = Regex("""(19|20)\d{2}""")

/** Top tabs of the phone Home screen. They only filter which Home rows are shown. */
enum class HomeTab(val label: StringResource) {
    HOME(Res.string.home_tab_home),
    MOVIES(Res.string.home_tab_movies),
    SERIES(Res.string.home_tab_series),
    SERVICES(Res.string.home_tab_services),
    ;

    /** Whether a catalog of [contentType] is shown on this tab. */
    fun showsCatalog(contentType: String): Boolean {
        val type = contentType.trim().lowercase()
        return when (this) {
            HOME -> true
            MOVIES -> type == "movie" || type == "movies"
            SERIES -> type == "series" || type == "tv" || type == "show"
            SERVICES -> false
        }
    }

    val showsCollections: Boolean get() = this == HOME || this == SERVICES
    val showsContinueWatching: Boolean get() = this == HOME

    /** Featured items for this tab, falling back to all of them when none match. */
    fun filterFeatured(items: List<MetaPreview>): List<MetaPreview> {
        if (this == HOME || this == SERVICES) return items
        return items.filter { showsCatalog(it.type) }.ifEmpty { items }
    }
}

/**
 * Phone Home banner: one large rounded "spotlight" card at a time (swipe or wait to change),
 * with the card's artwork blurred behind it so the page takes on the title's colours, a frosted
 * tab pill above and page dots below.
 */
@Composable
fun HomeSpotlightHero(
    items: List<MetaPreview>,
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
    onItemClick: ((MetaPreview) -> Unit)? = null,
    onItemLongClick: ((MetaPreview) -> Unit)? = null,
) {
    if (items.isEmpty()) return
    val pagerState = key(items.size, items.firstOrNull()?.stableKey()) {
        rememberPagerState(
            initialPage = if (items.size > 1) Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % items.size else 0,
            pageCount = { if (items.size > 1) Int.MAX_VALUE else 1 },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val currentIndex = pagerState.currentPage % items.size
    val currentItem = items[currentIndex]
    val background = MaterialTheme.colorScheme.background

    ScreenActivityEffect(pagerState.settledPage, items.size) { active ->
        if (!active || items.size <= 1) return@ScreenActivityEffect
        delay(SPOTLIGHT_AUTO_ADVANCE_MS)
        while (pagerState.isScrollInProgress) delay(100L)
        pagerState.animateScrollToPage(pagerState.currentPage + 1)
    }

    // Clip so the enlarged, blurred backdrop can't spill below the banner.
    Box(modifier = modifier.fillMaxWidth().clipToBounds()) {
        // Blurred artwork of the current card fills the whole banner area behind it.
        Crossfade(
            targetState = currentItem,
            animationSpec = tween(700),
            modifier = Modifier.matchParentSize(),
            label = "SpotlightBackdrop",
        ) { item ->
            AsyncImage(
                model = item.banner ?: item.poster,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.2f; scaleY = 1.2f }
                    .blur(40.dp),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to background.copy(alpha = 0.45f),
                        0.35f to background.copy(alpha = 0.30f),
                        0.75f to background.copy(alpha = 0.80f),
                        1f to background,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeTabPill(selected = selectedTab, onSelect = onTabSelected)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                pageSpacing = 10.dp,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val item = items[page % items.size]
                val offset = abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
                SpotlightCard(
                    item = item,
                    onItemClick = onItemClick,
                    onItemLongClick = onItemLongClick,
                    modifier = Modifier.graphicsLayer {
                        val scale = 1f - 0.06f * offset
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - 0.5f * offset
                    },
                )
            }
            if (items.size > 1) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    items.indices.forEach { index ->
                        val active = index == currentIndex
                        val width by animateDpAsState(
                            targetValue = if (active) 22.dp else 6.dp,
                            animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
                            label = "SpotlightDot",
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            heroPageForItem(pagerState.currentPage, index, items.size),
                                        )
                                    }
                                }
                                .padding(vertical = 8.dp)
                                .width(width)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (active) 1f else 0.4f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTabPill(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF28282E).copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        HomeTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) Color.White.copy(alpha = 0.92f) else Color.Transparent,
                animationSpec = tween(220),
                label = "HomeTabContainer",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF08080A) else Color.White,
                animationSpec = tween(220),
                label = "HomeTabContent",
            )
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(containerColor)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(tab.label),
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SpotlightCard(
    item: MetaPreview,
    onItemClick: ((MetaPreview) -> Unit)?,
    onItemLongClick: ((MetaPreview) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    var logoFailed by remember(item.id, item.logo) { mutableStateOf(false) }
    val logoUrl = item.logo?.takeIf { it.isNotBlank() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .aspectRatio(0.72f)
            .shadow(26.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            .clickable(enabled = onItemClick != null) { onItemClick?.invoke(item) },
    ) {
        AsyncImage(
            model = item.banner ?: item.poster,
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.42f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.88f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                text = spotlightTag(item),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                maxLines = 1,
            )
            if (logoUrl != null && !logoFailed) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(2.8f),
                    contentScale = ContentScale.Fit,
                    onError = { logoFailed = true },
                )
            } else {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 34.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            item.genres.filter { it.isNotBlank() }.take(3).takeIf { it.isNotEmpty() }?.let { genres ->
                Text(
                    text = genres.joinToString(" · "),
                    color = Color(0xFFE5E5EA),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SpotlightButtons(item = item, onItemClick = onItemClick, onItemLongClick = onItemLongClick)
        }
    }
}

@Composable
private fun SpotlightButtons(
    item: MetaPreview,
    onItemClick: ((MetaPreview) -> Unit)?,
    onItemLongClick: ((MetaPreview) -> Unit)?,
) {
    val libraryUiState by LibraryRepository.uiState.collectAsStateWithLifecycle()
    val isSaved = remember(libraryUiState, item.id, item.type) { LibraryRepository.isSaved(item.id, item.type) }
    val isLocalLibrary = libraryUiState.sourceMode == LibrarySourceMode.LOCAL
    val pill = RoundedCornerShape(23.dp)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(pill)
                .background(Color(0xFFF5F5F7))
                .clickable(enabled = onItemClick != null) { onItemClick?.invoke(item) },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color(0xFF08080A), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(Res.string.action_play), color = Color(0xFF08080A), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(pill)
                .background(Color.White.copy(alpha = 0.16f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), pill)
                .clickable(enabled = isLocalLibrary || onItemLongClick != null) {
                    if (isLocalLibrary) {
                        LibraryRepository.toggleLocalSaved(item.toLibraryItem(savedAtEpochMs = 0L))
                    } else {
                        // Trakt/Simkl libraries: open the usual poster actions (list picker etc.).
                        onItemLongClick?.invoke(item)
                    }
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(Res.string.home_my_list), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun spotlightTag(item: MetaPreview): String {
    val currentYear = remember { 1970 + (LibraryClock.nowEpochMs() / MS_PER_AVERAGE_YEAR).toInt() }
    val releaseYear = remember(item.id, item.releaseInfo, item.rawReleaseDate) {
        (item.rawReleaseDate ?: item.releaseInfo)?.let { ReleaseYearRegex.find(it)?.value?.toIntOrNull() }
    }
    val type = item.type.trim().lowercase()
    val label = when {
        releaseYear != null && releaseYear >= currentYear -> stringResource(Res.string.home_spotlight_new)
        type == "movie" -> stringResource(Res.string.media_movie)
        type == "series" || type == "tv" -> stringResource(Res.string.media_series)
        else -> item.type
    }
    return label.uppercase()
}
