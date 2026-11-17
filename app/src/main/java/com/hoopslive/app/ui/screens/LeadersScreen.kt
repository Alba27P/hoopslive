package com.hoopslive.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoopslive.app.data.LeaderEntry
import com.hoopslive.app.data.NbaRepository
import com.hoopslive.app.ui.Fmt
import com.hoopslive.app.ui.components.AppTopBar
import com.hoopslive.app.ui.components.Card
import com.hoopslive.app.ui.components.Headshot
import com.hoopslive.app.ui.components.LoadContent
import com.hoopslive.app.ui.components.MessageBox
import com.hoopslive.app.ui.components.TeamLogo
import com.hoopslive.app.ui.rememberLoad
import com.hoopslive.app.ui.theme.Accent

@Composable
fun LeadersScreen(onPlayer: (String, String?) -> Unit) {
    val cats = NbaRepository.leaderCategories
    var key by rememberSaveable { mutableStateOf(cats.first().key) }
    val cat = cats.firstOrNull { it.key == key } ?: cats.first()
    val h = rememberLoad<List<LeaderEntry>>(key = "leaders-${cat.key}") { force -> NbaRepository.leaders(cat, force) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "League Leaders",
            subtitle = Fmt.seasonLabel(NbaRepository.currentSeasonYear()) + " regular season · per game",
            refreshing = h.refreshing,
            onRefresh = h.refresh,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(cats, key = { it.key }) { c ->
                FilterChip(selected = c.key == cat.key, onClick = { key = c.key }, label = { Text(c.title) })
            }
        }
        LoadContent(h) { list ->
            if (list.isEmpty()) {
                MessageBox("No qualified players yet for ${cat.title}.")
            } else {
                LazyColumn(contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp)) {
                    items(list) { e ->
                        LeaderRow(e, top = e.rank <= 3) { onPlayer(e.athleteId, e.teamId) }
                    }
                    item {
                        Text(
                            "Rate stats require minimum games and attempts, similar to NBA qualification rules.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderRow(e: LeaderEntry, top: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${e.rank}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (top) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(30.dp),
            )
            Headshot(e.headshot, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(e.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeamLogo(e.teamLogo, 16.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        listOfNotNull(e.teamAbbr, e.gamesPlayed?.let { "$it GP" }).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                e.value,
                style = if (top) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
