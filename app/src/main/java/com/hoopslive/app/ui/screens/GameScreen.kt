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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoopslive.app.data.Competitor
import com.hoopslive.app.data.GameDetail
import com.hoopslive.app.data.GameState
import com.hoopslive.app.data.NbaRepository
import com.hoopslive.app.data.Play
import com.hoopslive.app.data.TeamBox
import com.hoopslive.app.ui.Fmt
import com.hoopslive.app.ui.Load
import com.hoopslive.app.ui.components.AppTopBar
import com.hoopslive.app.ui.components.Card
import com.hoopslive.app.ui.components.FrozenTable
import com.hoopslive.app.ui.components.LiveDot
import com.hoopslive.app.ui.components.LoadContent
import com.hoopslive.app.ui.components.MessageBox
import com.hoopslive.app.ui.components.SectionHeader
import com.hoopslive.app.ui.components.TableRow
import com.hoopslive.app.ui.components.TeamLogo
import com.hoopslive.app.ui.rememberLoad
import com.hoopslive.app.ui.theme.LiveRed

private val TABS = listOf("Box Score", "Team Stats", "Plays", "Info")

@Composable
fun GameScreen(
    gameId: String,
    onBack: () -> Unit,
    onPlayer: (playerId: String, teamId: String?) -> Unit,
    onTeam: (String) -> Unit,
) {
    val h = rememberLoad<GameDetail>(
        key = "game-$gameId",
        poll = { if (it.game.state == GameState.LIVE) 15_000L else null },
    ) { force -> NbaRepository.gameDetail(gameId, force) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        val title = (h.state as? Load.Ok<GameDetail>)?.data?.game
            ?.let { "${it.away.team.abbr} @ ${it.home.team.abbr}" } ?: "Game"
        AppTopBar(title = title, onBack = onBack, refreshing = h.refreshing, onRefresh = h.refresh)
        LoadContent(h) { d ->
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item { ScoreHeader(d, onTeam) }
                item { LineScore(d) }
                item {
                    TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                        TABS.forEachIndexed { i, t ->
                            Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, maxLines = 1) })
                        }
                    }
                }
                when (tab) {
                    0 -> {
                        if (d.boxes.isEmpty() || d.boxes.all { b -> b.players.all { it.stats.isEmpty() } }) {
                            item { MessageBox("Box score will appear once the game tips off.") }
                        } else {
                            d.boxes.forEach { box ->
                                item { BoxTable(box, onPlayer) }
                            }
                        }
                    }
                    1 -> item { TeamStatsTab(d) }
                    2 -> playByPlay(d)
                    else -> item { InfoTab(d) }
                }
            }
        }
    }
}

@Composable
private fun ScoreHeader(d: GameDetail, onTeam: (String) -> Unit) {
    val g = d.game
    Card {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderTeam(g.away, Modifier.weight(1f), onTeam)
            Column(Modifier.weight(1.5f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (g.state == GameState.PRE) {
                    Text(Fmt.localDay(g.date), style = MaterialTheme.typography.labelLarge)
                    Text(Fmt.localTime(g.date), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            g.away.score ?: "-",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            " – ",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            g.home.score ?: "-",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (g.state == GameState.LIVE) {
                        LiveDot()
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        g.statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (g.state == GameState.LIVE) LiveRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (g.seriesNote != null) {
                    Text(g.seriesNote, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            HeaderTeam(g.home, Modifier.weight(1f), onTeam)
        }
    }
}

@Composable
private fun HeaderTeam(c: Competitor, modifier: Modifier, onTeam: (String) -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).clickable { onTeam(c.team.id) }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TeamLogo(c.team.logo, 56.dp)
        Spacer(Modifier.height(6.dp))
        Text(c.team.shortName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (c.record != null) {
            Text(c.record, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LineScore(d: GameDetail) {
    val g = d.game
    val periods = maxOf(g.away.linescores.size, g.home.linescores.size)
    if (periods == 0) return
    val cols = (0 until periods).map { Fmt.periodName(it) } + "T"
    fun cells(c: Competitor) = (0 until periods).map { c.linescores.getOrElse(it) { "-" } } + (c.score ?: "-")
    Card {
        FrozenTable(
            leadHeader = "",
            columns = cols,
            leadWidth = 90.dp,
            rowHeight = 36.dp,
            highlightColumn = cols.lastIndex,
            rows = listOf(g.away, g.home).map { c ->
                TableRow(cells = cells(c)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TeamLogo(c.team.logo, 20.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(c.team.abbr, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            modifier = Modifier.padding(vertical = 6.dp),
        )
    }
}

@Composable
private fun BoxTable(box: TeamBox, onPlayer: (String, String?) -> Unit) {
    val starters = box.players.filter { it.starter }
    val bench = box.players.filter { !it.starter }
    val rows = ArrayList<TableRow>()
    fun add(list: List<com.hoopslive.app.data.BoxPlayer>, label: String) {
        if (list.isEmpty()) return
        rows.add(TableRow(cells = emptyList(), section = true) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        })
        list.forEach { p ->
            val dnp = p.didNotPlay || p.stats.isEmpty()
            rows.add(
                TableRow(
                    cells = if (dnp) emptyList() else p.stats,
                    dim = dnp,
                    onClick = { onPlayer(p.id, box.team.id) },
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                p.shortName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (p.position != null) {
                                Text(
                                    " ${p.position}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (dnp) {
                            Text(
                                "DNP" + (p.reason?.let { " · " + it.lowercase().replaceFirstChar { c -> c.uppercase() } } ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
            )
        }
    }
    add(starters, "STARTERS")
    add(bench, "BENCH")
    if (box.totals.any { it.isNotBlank() }) {
        rows.add(TableRow(cells = box.totals, emphasis = true) {
            Text("TOTALS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        })
    }
    Column {
        Row(Modifier.padding(start = 16.dp, top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TeamLogo(box.team.logo, 24.dp)
            Spacer(Modifier.width(8.dp))
            Text(box.team.name, style = MaterialTheme.typography.titleMedium)
        }
        val ptsCol = box.labels.indexOf("PTS")
        FrozenTable(leadHeader = "PLAYER", columns = box.labels, rows = rows, leadWidth = 150.dp, highlightColumn = ptsCol)
    }
}

@Composable
private fun TeamStatsTab(d: GameDetail) {
    val g = d.game
    if (d.teamStats.isEmpty()) {
        MessageBox("Team stats will appear once the game tips off.")
        return
    }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamLogo(g.away.team.logo, 28.dp)
            Text(g.away.team.abbr, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            Spacer(Modifier.weight(1f))
            Text(g.home.team.abbr, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
            TeamLogo(g.home.team.logo, 28.dp)
        }
        Spacer(Modifier.height(8.dp))
        d.teamStats.forEach { s ->
            val a = s.away.substringBefore('-').toDoubleOrNull()
            val b = s.home.substringBefore('-').toDoubleOrNull()
            val lowerIsBetter = s.label.contains("Turnover", true) || s.label.contains("Foul", true)
            val awayBetter = a != null && b != null && (if (lowerIsBetter) a < b else a > b)
            val homeBetter = a != null && b != null && (if (lowerIsBetter) b < a else b > a)
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                StatValue(s.away, awayBetter, Modifier.width(72.dp), TextAlign.Start)
                Text(
                    s.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                StatValue(s.home, homeBetter, Modifier.width(72.dp), TextAlign.End)
            }
            if (a != null && b != null && a + b > 0) {
                Row(Modifier.fillMaxWidth().height(4.dp)) {
                    val frac = (a / (a + b)).toFloat().coerceIn(0.02f, 0.98f)
                    Box(
                        Modifier.weight(frac).fillMaxSize().clip(RoundedCornerShape(2.dp))
                            .background(if (awayBetter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier.weight(1f - frac).fillMaxSize().clip(RoundedCornerShape(2.dp))
                            .background(if (homeBetter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatValue(v: String, better: Boolean, modifier: Modifier, align: TextAlign) {
    Text(
        v,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (better) FontWeight.Bold else FontWeight.Normal,
        color = if (better) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        textAlign = align,
        modifier = modifier,
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.playByPlay(d: GameDetail) {
    if (d.plays.isEmpty()) {
        item { MessageBox("Play-by-play will appear once the game tips off.") }
        return
    }
    val g = d.game
    val logos = mapOf(g.away.team.id to g.away.team.logo, g.home.team.id to g.home.team.logo)
    val byPeriod = d.plays.groupBy { it.period }.toSortedMap(compareByDescending<Int> { it })
    byPeriod.forEach { (period, plays) ->
        val label = plays.first().periodLabel.ifBlank { "Period $period" }
        item { SectionHeader(label) }
        items(plays.asReversed()) { p -> PlayRow(p, p.teamId?.let { logos[it] }) }
    }
}

@Composable
private fun PlayRow(p: Play, logo: String?) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            p.clock,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp),
        )
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            if (logo != null) TeamLogo(logo, 20.dp)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            p.text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (p.scoring) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "${p.awayScore}-${p.homeScore}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (p.scoring) FontWeight.Bold else FontWeight.Normal,
            color = if (p.scoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun InfoTab(d: GameDetail) {
    val g = d.game
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        InfoLine("Tip-off", "${Fmt.localDay(g.date)} · ${Fmt.localTime(g.date)}")
        d.venue?.let { InfoLine("Arena", it) }
        d.attendance?.let { InfoLine("Attendance", "%,d".format(it)) }
        g.broadcast?.let { InfoLine("TV", it) }
        if (d.officials.isNotEmpty()) InfoLine("Officials", d.officials.joinToString(", "))
        g.headline?.let { InfoLine("Recap", it) }
    }
}

@Composable
fun InfoLine(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
