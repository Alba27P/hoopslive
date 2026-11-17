package com.hoopslive.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hoopslive.app.data.Contract
import com.hoopslive.app.data.GameLog
import com.hoopslive.app.data.NbaRepository
import com.hoopslive.app.data.StatTable
import com.hoopslive.app.ui.Fmt
import com.hoopslive.app.ui.Load
import com.hoopslive.app.ui.components.AppTopBar
import com.hoopslive.app.ui.components.Card
import com.hoopslive.app.ui.components.FrozenTable
import com.hoopslive.app.ui.components.Headshot
import com.hoopslive.app.ui.components.LoadContent
import com.hoopslive.app.ui.components.MessageBox
import com.hoopslive.app.ui.components.Pill
import com.hoopslive.app.ui.components.SectionHeader
import com.hoopslive.app.ui.components.TableRow
import com.hoopslive.app.ui.components.TeamLogo
import com.hoopslive.app.ui.rememberLoad
import com.hoopslive.app.ui.theme.LossRed
import com.hoopslive.app.ui.theme.WinGreen

private val PLAYER_TABS = listOf("Overview", "Stats", "Game Log", "Contract")

@Composable
fun PlayerScreen(
    playerId: String,
    teamHint: String?,
    onBack: () -> Unit,
    onTeam: (String) -> Unit,
    onGame: (String) -> Unit,
) {
    val profile = rememberLoad<NbaRepository.PlayerProfile>(key = "player-$playerId") {
        NbaRepository.playerProfile(playerId, teamHint)
    }
    val stats = rememberLoad<List<StatTable>>(key = "pstats-$playerId") { NbaRepository.playerStats(playerId) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val name = (profile.state as? Load.Ok<NbaRepository.PlayerProfile>)?.data?.bio?.name ?: "Player"

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        AppTopBar(title = name, onBack = onBack)
        LoadContent(profile) { p -> PlayerHeader(p, onTeam) }
        ScrollableTabRow(
            selectedTabIndex = tab,
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            PLAYER_TABS.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> Column(Modifier.verticalScroll(rememberScrollState())) {
                LoadContent(stats) { tables -> SeasonAverages(tables) }
                LoadContent(profile) { p -> Bio(p) }
                LoadContent(profile) { p -> ContractSummary(p.contract) }
                Spacer(Modifier.height(16.dp))
            }
            1 -> LoadContent(stats) { tables -> CareerStats(tables) }
            2 -> GameLogTab(playerId, onGame)
            else -> LoadContent(profile) { p ->
                Column(Modifier.verticalScroll(rememberScrollState())) { ContractDetail(p.contract) }
            }
        }
    }
}

@Composable
private fun PlayerHeader(p: NbaRepository.PlayerProfile, onTeam: (String) -> Unit) {
    val b = p.bio
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Headshot(b.headshot, 84.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(b.name, style = MaterialTheme.typography.titleLarge)
            Text(
                listOfNotNull(b.jersey?.let { "#$it" }, b.position).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val team = p.team
            if (team != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onTeam(team.id) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    TeamLogo(team.logo, 18.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(team.name, style = MaterialTheme.typography.labelLarge)
                }
            } else if (!b.active) {
                Text("Inactive / free agent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------- Overview ----------------

private val HEADLINE_STATS = listOf("PTS", "REB", "AST", "FG%", "3P%", "FT%", "STL", "BLK", "MIN")

@Composable
private fun SeasonAverages(tables: List<StatTable>) {
    val avg = tables.firstOrNull { it.name == "averages" } ?: tables.firstOrNull() ?: return
    val latest = avg.rows.firstOrNull() ?: return
    val current = latest.year == NbaRepository.currentSeasonYear()
    SectionHeader(
        if (current) "${latest.season} season" else "Last season played · ${latest.season}",
        trailing = listOfNotNull(latest.team, avg.labels.indexOf("GP").takeIf { it >= 0 }?.let { "${latest.stats.getOrNull(it)} GP" })
            .joinToString(" · "),
    )
    FlowRow(
        modifier = Modifier.padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3,
    ) {
        HEADLINE_STATS.forEach { label ->
            val i = avg.labels.indexOf(label)
            val v = if (i >= 0) latest.stats.getOrNull(i) else null
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(v ?: "-", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Bio(p: NbaRepository.PlayerProfile) {
    val b = p.bio
    val items = listOfNotNull(
        b.age?.let { "Age" to "$it" },
        b.birthDate?.let { "Born" to (Fmt.isoDate(it) ?: it) },
        b.birthPlace?.let { "Birthplace" to it },
        b.height?.let { "Height" to it },
        b.weight?.let { "Weight" to it },
        b.experience?.let { "Experience" to if (it == 0) "Rookie" else "$it yrs" },
        b.draft?.let { "Draft" to it },
        p.college?.let { "College" to it },
        b.hand?.let { "Shoots" to it },
    )
    if (items.isEmpty()) return
    SectionHeader("Bio")
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { (k, v) ->
                Row {
                    Text(k, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(110.dp))
                    Text(v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ContractSummary(c: Contract?) {
    SectionHeader("Contract")
    if (c == null) {
        Text(
            "No contract information available.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        return
    }
    Card {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Fmt.money(c.salary), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    c.seasonYear?.let { "${Fmt.seasonLabel(it)} salary" } ?: "Current salary",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            c.yearsRemaining?.let { y ->
                Column(horizontalAlignment = Alignment.End) {
                    Text("$y", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(if (y == 1) "year left" else "years left", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ---------------- Contract tab ----------------

@Composable
private fun ContractDetail(c: Contract?) {
    if (c == null) {
        MessageBox("No contract information available for this player.")
        return
    }
    ContractSummary(c)
    if (c.tradeKicker) {
        Text(
            "Contract includes a trade kicker.",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    if (c.history.isNotEmpty()) {
        val max = c.history.maxOf { it.salary }.coerceAtLeast(1L)
        SectionHeader("Salary by season", trailing = "Career ${Fmt.moneyShort(c.history.sumOf { it.salary })}")
        c.history.forEach { y ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Fmt.seasonLabel(y.seasonYear), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(64.dp))
                Box(Modifier.weight(1f).height(18.dp)) {
                    val frac = (y.salary.toFloat() / max.toFloat()).coerceIn(0.02f, 1f)
                    Box(
                        Modifier
                            .fillMaxWidth(frac)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (y.seasonYear == c.seasonYear) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
                Text(
                    Fmt.money(y.salary),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(110.dp),
                )
            }
        }
        Text(
            "Salary figures from ESPN. Future seasons appear once included in ESPN's contract data.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

// ---------------- Career stats ----------------

@Composable
private fun CareerStats(tables: List<StatTable>) {
    if (tables.isEmpty()) {
        MessageBox("No NBA stats yet.")
        return
    }
    var which by rememberSaveable { mutableIntStateOf(0) }
    val t = tables[which.coerceIn(0, tables.lastIndex)]
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tables.forEachIndexed { i, tb ->
                val label = when (tb.name) {
                    "averages" -> "Averages"
                    "totals" -> "Totals"
                    "miscellaneous" -> "Misc"
                    else -> tb.title
                }
                FilterChip(selected = i == which, onClick = { which = i }, label = { Text(label) })
            }
        }
        val rows = t.rows.map { r ->
            TableRow(cells = r.stats) {
                Column {
                    Text(r.season, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    if (r.team != null) {
                        Text(r.team, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } + if (t.totals.isNotEmpty()) listOf(
            TableRow(cells = t.totals, emphasis = true) {
                Text("CAREER", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        ) else emptyList()
        FrozenTable(
            leadHeader = "SEASON",
            columns = t.labels,
            rows = rows,
            leadWidth = 92.dp,
            rowHeight = 44.dp,
            highlightColumn = t.labels.indexOf("PTS"),
        )
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------- Game log ----------------

@Composable
private fun GameLogTab(playerId: String, onGame: (String) -> Unit) {
    val h = rememberLoad<GameLog>(key = "gamelog-$playerId") { NbaRepository.gameLog(playerId) }
    LoadContent(h) { log ->
        if (log.rows.isEmpty()) {
            MessageBox("No games played this season yet.")
            return@LoadContent
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SectionHeader("Most recent first", trailing = "${log.rows.size} games")
            FrozenTable(
                leadHeader = "GAME",
                columns = log.labels,
                leadWidth = 128.dp,
                rowHeight = 44.dp,
                highlightColumn = log.labels.indexOf("PTS"),
                rows = log.rows.map { r ->
                    TableRow(cells = r.stats, onClick = { onGame(r.eventId) }) {
                        Column {
                            Text(Fmt.localDay(r.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${r.atVs} ", style = MaterialTheme.typography.labelMedium)
                                TeamLogo(r.opponentLogo, 16.dp)
                                Text(" ${r.opponent} ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                r.result?.takeIf { it == "W" || it == "L" }?.let { res ->
                                    Pill(res, if (res == "W") WinGreen else LossRed)
                                }
                            }
                        }
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
