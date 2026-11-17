package com.hoopslive.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoopslive.app.data.Conference
import com.hoopslive.app.data.NbaRepository
import com.hoopslive.app.data.StandingRow
import com.hoopslive.app.ui.Fmt
import com.hoopslive.app.ui.components.AppTopBar
import com.hoopslive.app.ui.components.FrozenTable
import com.hoopslive.app.ui.components.LoadContent
import com.hoopslive.app.ui.components.MessageBox
import com.hoopslive.app.ui.components.TableRow
import com.hoopslive.app.ui.components.TeamLogo
import com.hoopslive.app.ui.rememberLoad
import com.hoopslive.app.ui.theme.PlayInOrange
import com.hoopslive.app.ui.theme.PlayoffGreen

private val COLS = listOf("W", "L", "PCT", "GB", "HOME", "AWAY", "CONF", "L10", "STRK", "PPG", "OPP", "DIFF")

@Composable
fun StandingsScreen(onTeam: (String) -> Unit) {
    val h = rememberLoad<List<Conference>>(key = "standings") { force -> NbaRepository.standings(force) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Standings",
            subtitle = Fmt.seasonLabel(NbaRepository.currentSeasonYear()) + " regular season",
            refreshing = h.refreshing,
            onRefresh = h.refresh,
        )
        LoadContent(h) { confs ->
            if (confs.isEmpty()) {
                MessageBox("Standings are not available yet.")
                return@LoadContent
            }
            val tabs = confs.map { it.name.replace(" Conference", "") } + "League"
            val safeTab = tab.coerceIn(0, tabs.lastIndex)
            TabRow(selectedTabIndex = safeTab, containerColor = MaterialTheme.colorScheme.background) {
                tabs.forEachIndexed { i, t -> Tab(selected = safeTab == i, onClick = { tab = i }, text = { Text(t) }) }
            }
            val isLeague = safeTab == tabs.lastIndex
            val rows: List<StandingRow> = if (isLeague) {
                confs.flatMap { it.rows }.sortedWith(compareByDescending<StandingRow> { it.winPct }.thenByDescending { it.wins })
            } else {
                confs[safeTab].rows
            }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FrozenTable(
                    leadHeader = "TEAM",
                    columns = COLS,
                    leadWidth = 128.dp,
                    highlightColumn = 2,
                    rows = rows.mapIndexed { i, r ->
                        val pos = i + 1
                        TableRow(
                            cells = listOf(
                                r.wins.toString(), r.losses.toString(), r.pct, r.gb, r.home, r.road,
                                r.conf, r.lastTen, r.streak, r.ppg, r.oppPpg, r.diff,
                            ),
                            onClick = { onTeam(r.team.id) },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val bar = when {
                                    isLeague -> Color.Transparent
                                    pos <= 6 -> PlayoffGreen
                                    pos <= 10 -> PlayInOrange
                                    else -> Color.Transparent
                                }
                                Box(Modifier.width(3.dp).fillMaxHeight().padding(vertical = 8.dp).background(bar))
                                Text(
                                    "$pos",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(26.dp).padding(start = 6.dp),
                                )
                                TeamLogo(r.team.logo, 22.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(r.team.abbr, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                if (r.clincher != null) {
                                    Text(
                                        " ${r.clincher}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (!isLeague) Legend()
            }
        }
    }
}

@Composable
private fun Legend() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LegendItem(PlayoffGreen, "Playoffs (1–6)")
        LegendItem(PlayInOrange, "Play-In Tournament (7–10)")
        Text(
            "x / y / z / * : clinched playoff berth / division / conference / best record · e : eliminated",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}
