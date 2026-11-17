package com.hoopslive.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoopslive.app.data.Competitor
import com.hoopslive.app.data.Game
import com.hoopslive.app.data.GameState
import com.hoopslive.app.data.NbaRepository
import com.hoopslive.app.ui.Fmt
import com.hoopslive.app.ui.components.AppTopBar
import com.hoopslive.app.ui.components.Card
import com.hoopslive.app.ui.components.LiveDot
import com.hoopslive.app.ui.components.LoadContent
import com.hoopslive.app.ui.components.MessageBox
import com.hoopslive.app.ui.components.SectionHeader
import com.hoopslive.app.ui.components.TeamLogo
import com.hoopslive.app.ui.rememberLoad
import com.hoopslive.app.ui.theme.LiveRed
import java.time.LocalDate

private const val DAYS_EACH_SIDE = 45

@Composable
fun ScoresScreen(onGame: (String) -> Unit) {
    val today = remember { NbaRepository.todayGameDay() }
    var epochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val day = LocalDate.ofEpochDay(epochDay)

    val h = rememberLoad<List<Game>>(
        key = "scores-$epochDay",
        poll = { games ->
            when {
                games.any { it.state == GameState.LIVE } -> 20_000L
                day == today -> 60_000L
                else -> null
            }
        },
    ) { force -> NbaRepository.scoreboard(day, force) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Hoops Live",
            subtitle = "NBA · ${Fmt.day(day)}",
            refreshing = h.refreshing,
            onRefresh = h.refresh,
        )
        DateStrip(
            selected = day,
            today = today,
            onSelect = { epochDay = it.toEpochDay() },
        )
        LoadContent(h) { games ->
            if (games.isEmpty()) {
                MessageBox("No NBA games on ${Fmt.day(day)}")
            } else {
                val live = games.filter { it.state == GameState.LIVE }
                val upcoming = games.filter { it.state == GameState.PRE }
                val done = games.filter { it.state == GameState.FINAL || it.state == GameState.OTHER }
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    if (live.isNotEmpty()) {
                        item { SectionHeader("Live", trailing = "${live.size}") }
                        items(live) { GameCard(it) { onGame(it.id) } }
                    }
                    if (upcoming.isNotEmpty()) {
                        item { SectionHeader("Upcoming", trailing = "Times in your time zone") }
                        items(upcoming) { GameCard(it) { onGame(it.id) } }
                    }
                    if (done.isNotEmpty()) {
                        item { SectionHeader("Final") }
                        items(done) { GameCard(it) { onGame(it.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateStrip(selected: LocalDate, today: LocalDate, onSelect: (LocalDate) -> Unit) {
    val days = remember(today) { (-DAYS_EACH_SIDE..DAYS_EACH_SIDE).map { today.plusDays(it.toLong()) } }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        val idx = days.indexOf(selected).coerceAtLeast(0)
        listState.scrollToItem((idx - 2).coerceAtLeast(0))
    }
    Column {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(days) { _, d ->
                val isSel = d == selected
                val bg = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer
                val fg = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .clickable { onSelect(d) }
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        if (d == today) "TODAY" else Fmt.weekday(d),
                        style = MaterialTheme.typography.labelSmall,
                        color = fg.copy(alpha = 0.8f),
                    )
                    Text(Fmt.dayNumber(d), style = MaterialTheme.typography.titleMedium, color = fg, fontWeight = FontWeight.Bold)
                    Text(Fmt.month(d), style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.7f))
                }
            }
        }
        if (selected != today) {
            TextButton(onClick = { onSelect(today) }, modifier = Modifier.padding(start = 8.dp)) {
                Text("Jump to today")
            }
        } else {
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
fun GameCard(g: Game, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 14.dp, end = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TeamLine(g.away, g)
                TeamLine(g.home, g)
            }
            VerticalDivider(Modifier.height(52.dp))
            Box(Modifier.width(96.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                StatusBlock(g)
            }
        }
    }
}

@Composable
private fun TeamLine(c: Competitor, g: Game) {
    val loser = g.state == GameState.FINAL && !c.winner && (g.away.winner || g.home.winner)
    val color = if (loser) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(verticalAlignment = Alignment.CenterVertically) {
        TeamLogo(c.team.logo, 28.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                c.team.shortName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (c.winner) FontWeight.Bold else FontWeight.SemiBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (c.record != null) {
                Text(c.record, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (g.state != GameState.PRE && c.score != null) {
            Text(
                c.score,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (c.winner || g.state == GameState.LIVE) FontWeight.Bold else FontWeight.Medium,
                color = color,
            )
        }
    }
}

@Composable
private fun StatusBlock(g: Game) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (g.state) {
            GameState.LIVE -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveDot()
                    Spacer(Modifier.width(6.dp))
                    Text("LIVE", style = MaterialTheme.typography.labelSmall, color = LiveRed, fontWeight = FontWeight.Bold)
                }
                Text(g.statusText, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = LiveRed)
            }
            GameState.PRE -> {
                Text(Fmt.localTime(g.date), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (g.broadcast != null) {
                    Text(
                        g.broadcast,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            else -> Text(
                g.statusText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (g.seriesNote != null) {
            Text(
                g.seriesNote,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8AB4F8),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
