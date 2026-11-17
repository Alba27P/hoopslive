package com.hoopslive.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoopslive.app.data.GameState
import com.hoopslive.app.data.NbaRepository
import com.hoopslive.app.data.RosterPlayer
import com.hoopslive.app.data.ScheduleGame
import com.hoopslive.app.data.StatCategory
import com.hoopslive.app.data.StatItem
import com.hoopslive.app.data.TeamInfo
import com.hoopslive.app.ui.Fmt
import com.hoopslive.app.ui.Load
import com.hoopslive.app.ui.components.AppTopBar
import com.hoopslive.app.ui.components.Card
import com.hoopslive.app.ui.components.Headshot
import com.hoopslive.app.ui.components.LiveDot
import com.hoopslive.app.ui.components.LoadContent
import com.hoopslive.app.ui.components.MessageBox
import com.hoopslive.app.ui.components.Pill
import com.hoopslive.app.ui.components.SectionHeader
import com.hoopslive.app.ui.components.TeamLogo
import com.hoopslive.app.ui.rememberLoad
import com.hoopslive.app.ui.theme.LiveRed
import com.hoopslive.app.ui.theme.LossRed
import com.hoopslive.app.ui.theme.WinGreen

private val TEAM_TABS = listOf("Schedule", "Roster", "Stats")

@Composable
fun TeamScreen(
    teamId: String,
    onBack: () -> Unit,
    onGame: (String) -> Unit,
    onPlayer: (String, String?) -> Unit,
) {
    val info = rememberLoad<TeamInfo>(key = "team-$teamId") { NbaRepository.teamInfo(teamId) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val title = (info.state as? Load.Ok<TeamInfo>)?.data?.team?.name ?: "Team"

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        AppTopBar(title = title, onBack = onBack)
        LoadContent(info) { TeamHeader(it) }
        TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            TEAM_TABS.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> ScheduleTab(teamId, onGame)
            1 -> RosterTab(teamId, onPlayer)
            else -> TeamStatsTab(teamId)
        }
    }
}

@Composable
private fun TeamHeader(i: TeamInfo) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TeamLogo(i.team.logo, 64.dp)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(i.record ?: "-", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            i.standingSummary?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            val split = listOfNotNull(i.homeRecord?.let { "Home $it" }, i.roadRecord?.let { "Away $it" }).joinToString(" · ")
            if (split.isNotBlank()) {
                Text(split, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------- Schedule ----------------

@Composable
private fun ScheduleTab(teamId: String, onGame: (String) -> Unit) {
    var seasonType by rememberSaveable { mutableIntStateOf(2) }
    val h = rememberLoad<List<ScheduleGame>>(
        key = "schedule-$teamId-$seasonType",
        poll = { list -> if (list.any { it.state == GameState.LIVE }) 30_000L else null },
    ) { NbaRepository.schedule(teamId, seasonType) }
    Column {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = seasonType == 2, onClick = { seasonType = 2 }, label = { Text("Regular season") })
            FilterChip(selected = seasonType == 3, onClick = { seasonType = 3 }, label = { Text("Playoffs") })
        }
        LoadContent(h) { games ->
            if (games.isEmpty()) {
                MessageBox(if (seasonType == 3) "No playoff games yet." else "No games scheduled.")
                return@LoadContent
            }
            val listState = rememberLazyListState()
            LaunchedEffect(games.size) {
                val next = games.indexOfFirst { it.state != GameState.FINAL }
                if (next > 2) listState.scrollToItem(next - 2)
            }
            val record = games.count { it.won == true } to games.count { it.won == false }
            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
                item {
                    Text(
                        "${games.size} games · ${record.first}-${record.second} in completed games",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                items(games) { g -> ScheduleRow(g) { onGame(g.id) } }
            }
        }
    }
}

@Composable
private fun ScheduleRow(g: ScheduleGame, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(92.dp)) {
                Text(Fmt.localDay(g.date), style = MaterialTheme.typography.labelLarge)
                if (g.state == GameState.PRE) {
                    Text(Fmt.localTime(g.date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                if (g.isHome) "vs" else "@",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(26.dp),
            )
            TeamLogo(g.opponent.logo, 26.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                g.opponent.shortName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            when {
                g.state == GameState.LIVE -> Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveDot()
                    Spacer(Modifier.width(6.dp))
                    Text("${g.teamScore ?: "-"}-${g.oppScore ?: "-"}", color = LiveRed, fontWeight = FontWeight.Bold)
                }
                g.won != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(if (g.won) "W" else "L", if (g.won) WinGreen else LossRed)
                    Spacer(Modifier.width(8.dp))
                    Text("${g.teamScore ?: "-"}-${g.oppScore ?: "-"}", fontWeight = FontWeight.SemiBold)
                }
                g.state == GameState.PRE -> Text(
                    "Upcoming",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Text(g.statusText, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ---------------- Roster ----------------

@Composable
private fun RosterTab(teamId: String, onPlayer: (String, String?) -> Unit) {
    val h = rememberLoad<List<RosterPlayer>>(key = "roster-$teamId") { NbaRepository.roster(teamId) }
    LoadContent(h) { players ->
        val sorted = players.sortedByDescending { it.contract?.salary ?: -1L }
        val payroll = players.sumOf { it.contract?.salary ?: 0L }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                SectionHeader(
                    "${players.size} players",
                    trailing = if (payroll > 0) "Payroll ${Fmt.moneyShort(payroll)}" else null,
                )
            }
            items(sorted) { p -> RosterRow(p) { onPlayer(p.id, teamId) } }
        }
    }
}

@Composable
private fun RosterRow(p: RosterPlayer, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Headshot(p.headshot, 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        p.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (p.injury != null) {
                        Spacer(Modifier.width(6.dp))
                        Pill(p.injury.uppercase(), LossRed)
                    }
                }
                Text(
                    listOfNotNull(
                        p.jersey?.let { "#$it" },
                        p.position,
                        p.age?.let { "$it yrs" },
                        p.height,
                        p.weight,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Fmt.moneyShort(p.contract?.salary), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                p.contract?.yearsRemaining?.let { y ->
                    Text(
                        if (y == 1) "1 yr left" else "$y yrs left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------------- Team stats ----------------

private val KEY_TEAM_STATS = listOf(
    "avgPoints" to "Points",
    "avgRebounds" to "Rebounds",
    "avgAssists" to "Assists",
    "fieldGoalPct" to "FG%",
    "threePointFieldGoalPct" to "3P%",
    "freeThrowPct" to "FT%",
    "avgSteals" to "Steals",
    "avgBlocks" to "Blocks",
    "avgTurnovers" to "Turnovers",
    "avgOffensiveRebounds" to "Off. Reb",
    "avgDefensiveRebounds" to "Def. Reb",
    "assistTurnoverRatio" to "AST/TO",
)

@Composable
private fun TeamStatsTab(teamId: String) {
    val h = rememberLoad<List<StatCategory>>(key = "teamstats-$teamId") { NbaRepository.teamStats(teamId) }
    LoadContent(h) { cats ->
        if (cats.isEmpty()) {
            MessageBox("Season stats are not available yet.")
            return@LoadContent
        }
        val all = cats.flatMap { it.stats }
        val keyStats = KEY_TEAM_STATS.mapNotNull { (name, label) ->
            all.firstOrNull { it.name == name }?.let { label to it }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                SectionHeader(
                    "Per game",
                    trailing = Fmt.seasonLabel(NbaRepository.currentSeasonYear()) + " · league rank",
                )
            }
            item { KeyStatGrid(keyStats) }
            cats.forEach { c ->
                item { SectionHeader(c.displayName) }
                items(c.stats) { s -> StatLine(s) }
            }
        }
    }
}

@Composable
private fun KeyStatGrid(stats: List<Pair<String, StatItem>>) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3,
    ) {
        stats.forEach { (label, s) ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(10.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(s.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    s.rank?.let { "Rank $it" } ?: " ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun StatLine(s: StatItem) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(s.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(s.value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            s.rank ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(78.dp).padding(start = 12.dp),
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
