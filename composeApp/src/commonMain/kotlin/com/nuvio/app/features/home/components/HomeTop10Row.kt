package com.nuvio.app.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.stableKey
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.home_top10_title
import org.jetbrains.compose.resources.stringResource

/**
 * Netflix-style "Top 10" row with big outlined numbers next to the posters. Uses the first ten
 * titles of one of the Home catalogs (normally its first popular catalog), and names it.
 */
@Composable
fun HomeTop10Row(
    section: HomeCatalogSection,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 20.dp,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
) {
    val entries = section.items.take(10)
    if (entries.size < 3) return
    val numberStyle = TextStyle(
        fontSize = 96.sp,
        lineHeight = 96.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.background,
    )
    val outlineStyle = numberStyle.copy(
        color = Color(0xFF8E8E93),
        drawStyle = Stroke(width = 5f),
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.home_top10_title, section.title),
            modifier = Modifier.padding(horizontal = horizontalPadding),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
            modifier = Modifier.padding(top = 8.dp),
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(entries, key = { _, item -> item.stableKey() }) { index, item ->
                Row(
                    modifier = Modifier.clickable(enabled = onPosterClick != null) { onPosterClick?.invoke(item) },
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Text(text = "${index + 1}", style = numberStyle)
                        Text(text = "${index + 1}", style = outlineStyle)
                    }
                    AsyncImage(
                        model = item.poster ?: item.banner,
                        contentDescription = item.name,
                        modifier = Modifier
                            .offset(x = (-12).dp)
                            .size(width = 92.dp, height = 136.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}
