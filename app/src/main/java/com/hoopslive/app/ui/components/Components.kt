package com.hoopslive.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hoopslive.app.ui.Load
import com.hoopslive.app.ui.LoadHandle
import com.hoopslive.app.ui.theme.LiveRed

@Composable
fun TeamLogo(url: String?, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}

@Composable
fun Headshot(url: String?, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun LiveDot(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "live")
    val a by t.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "liveAlpha",
    )
    Box(
        modifier
            .size(8.dp)
            .alpha(a)
            .clip(CircleShape)
            .background(LiveRed)
    )
}

@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    refreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
) {
    TopAppBar(
        title = {
            Column {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            if (onRefresh != null) {
                if (refreshing) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(20.dp),
                    )
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun MessageBox(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (action != null && onAction != null) {
            FilledTonalButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
fun <T> LoadContent(h: LoadHandle<T>, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    when (val s = h.state) {
        Load.Loading -> LoadingBox(modifier)
        is Load.Err -> MessageBox(s.message, modifier, "Retry", h.refresh)
        is Load.Ok -> content(s.data)
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) { content() }
}

// ---------------- Table with a frozen first column ----------------

class TableRow(
    val cells: List<String>,
    val emphasis: Boolean = false,
    val section: Boolean = false,
    val dim: Boolean = false,
    val onClick: (() -> Unit)? = null,
    val lead: @Composable () -> Unit,
)

/**
 * A stats table whose first column stays put while the stat columns scroll horizontally,
 * like box scores on ESPN/Sofascore.
 */
@Composable
fun FrozenTable(
    leadHeader: String,
    columns: List<String>,
    rows: List<TableRow>,
    modifier: Modifier = Modifier,
    leadWidth: Dp = 150.dp,
    rowHeight: Dp = 40.dp,
    highlightColumn: Int = -1,
) {
    val widths = columns.indices.map { i ->
        val len = maxOf(columns[i].length, rows.maxOfOrNull { it.cells.getOrNull(i)?.length ?: 0 } ?: 0)
        (len * 8 + 18).coerceAtLeast(38).dp
    }
    val zebra = MaterialTheme.colorScheme.surfaceContainerHigh
    val sectionBg = MaterialTheme.colorScheme.surfaceVariant
    fun bg(i: Int, r: TableRow): Color = when {
        r.section -> sectionBg
        i % 2 == 1 -> zebra
        else -> Color.Transparent
    }
    val headerStyle = MaterialTheme.typography.labelSmall
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier.fillMaxWidth()) {
        Column(Modifier.width(leadWidth)) {
            Box(Modifier.height(32.dp).fillMaxWidth().padding(start = 12.dp), contentAlignment = Alignment.CenterStart) {
                Text(leadHeader, style = headerStyle, color = muted)
            }
            rows.forEachIndexed { i, r ->
                Box(
                    Modifier
                        .height(rowHeight)
                        .fillMaxWidth()
                        .background(bg(i, r))
                        .then(if (r.onClick != null) Modifier.clickable(onClick = r.onClick) else Modifier)
                        .padding(start = 12.dp, end = 4.dp),
                    contentAlignment = Alignment.CenterStart,
                ) { r.lead() }
            }
        }
        Column(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
            Row(Modifier.height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                columns.forEachIndexed { c, h ->
                    Text(
                        h,
                        style = headerStyle,
                        color = if (c == highlightColumn) MaterialTheme.colorScheme.primary else muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(widths[c]),
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            rows.forEachIndexed { i, r ->
                Row(
                    Modifier.height(rowHeight).background(bg(i, r)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    columns.indices.forEach { c ->
                        Text(
                            r.cells.getOrNull(c) ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (r.emphasis || c == highlightColumn) FontWeight.Bold else FontWeight.Normal,
                            color = if (r.dim) muted else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.width(widths[c]),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
    }
}
