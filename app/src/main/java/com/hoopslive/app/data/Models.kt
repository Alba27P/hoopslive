package com.hoopslive.app.data

import java.time.Instant

data class TeamRef(
    val id: String,
    val abbr: String,
    val name: String,
    val shortName: String,
    val logo: String?,
    val color: String? = null,
)

enum class GameState { PRE, LIVE, FINAL, OTHER }

data class Competitor(
    val team: TeamRef,
    val score: String?,
    val record: String?,
    val isHome: Boolean,
    val winner: Boolean,
    val linescores: List<String>,
)

data class Game(
    val id: String,
    val date: Instant?,
    val state: GameState,
    val statusText: String,
    val away: Competitor,
    val home: Competitor,
    val broadcast: String?,
    val headline: String?,
    val seriesNote: String?,
)

// ---------- Game detail ----------

data class BoxPlayer(
    val id: String,
    val name: String,
    val shortName: String,
    val position: String?,
    val jersey: String?,
    val headshot: String?,
    val starter: Boolean,
    val didNotPlay: Boolean,
    val reason: String?,
    val stats: List<String>,
)

data class TeamBox(
    val team: TeamRef,
    val labels: List<String>,
    val players: List<BoxPlayer>,
    val totals: List<String>,
)

data class TeamStatLine(val label: String, val away: String, val home: String)

data class Play(
    val id: String,
    val period: Int,
    val periodLabel: String,
    val clock: String,
    val text: String,
    val awayScore: Int,
    val homeScore: Int,
    val scoring: Boolean,
    val teamId: String?,
)

data class GameDetail(
    val game: Game,
    val boxes: List<TeamBox>,
    val teamStats: List<TeamStatLine>,
    val plays: List<Play>,
    val venue: String?,
    val attendance: Int?,
    val officials: List<String>,
)

// ---------- Standings ----------

data class StandingRow(
    val team: TeamRef,
    val seed: Int,
    val wins: Int,
    val losses: Int,
    val pct: String,
    val gb: String,
    val home: String,
    val road: String,
    val conf: String,
    val lastTen: String,
    val streak: String,
    val ppg: String,
    val oppPpg: String,
    val diff: String,
    val clincher: String?,
    val winPct: Double,
)

data class Conference(val name: String, val rows: List<StandingRow>)

// ---------- Teams ----------

data class TeamInfo(
    val team: TeamRef,
    val record: String?,
    val standingSummary: String?,
    val homeRecord: String?,
    val roadRecord: String?,
)

data class ScheduleGame(
    val id: String,
    val date: Instant?,
    val opponent: TeamRef,
    val isHome: Boolean,
    val state: GameState,
    val statusText: String,
    val teamScore: String?,
    val oppScore: String?,
    val won: Boolean?,
)

data class Contract(
    val salary: Long?,
    val yearsRemaining: Int?,
    val seasonYear: Int?,
    val history: List<SalaryYear>,
    val tradeKicker: Boolean,
)

data class SalaryYear(val seasonYear: Int, val salary: Long)

data class RosterPlayer(
    val id: String,
    val name: String,
    val jersey: String?,
    val position: String?,
    val age: Int?,
    val height: String?,
    val weight: String?,
    val headshot: String?,
    val experience: Int?,
    val college: String?,
    val injury: String?,
    val contract: Contract?,
)

data class StatItem(
    val name: String,
    val label: String,
    val abbr: String,
    val value: String,
    val rank: String?,
)

data class StatCategory(val name: String, val displayName: String, val stats: List<StatItem>)

// ---------- Players ----------

data class PlayerBio(
    val id: String,
    val name: String,
    val headshot: String?,
    val position: String?,
    val jersey: String?,
    val age: Int?,
    val birthDate: String?,
    val birthPlace: String?,
    val height: String?,
    val weight: String?,
    val experience: Int?,
    val draft: String?,
    val teamId: String?,
    val collegeRef: String?,
    val contractsRef: String?,
    val active: Boolean,
    val hand: String?,
)

data class SeasonRow(val season: String, val year: Int, val team: String?, val stats: List<String>)

data class StatTable(
    val name: String,
    val title: String,
    val labels: List<String>,
    val rows: List<SeasonRow>,
    val totals: List<String>,
)

data class GameLogRow(
    val eventId: String,
    val date: Instant?,
    val atVs: String,
    val opponent: String,
    val opponentLogo: String?,
    val result: String?,
    val score: String?,
    val stats: List<String>,
)

data class GameLog(val labels: List<String>, val rows: List<GameLogRow>)

// ---------- Leaders ----------

data class LeaderCategory(
    val key: String,
    val title: String,
    val sort: String,
    val category: String,
    val stat: String,
    val minGamesShare: Double = 0.5,
    val minStat: Pair<String, Double>? = null,
)

data class LeaderEntry(
    val rank: Int,
    val athleteId: String,
    val name: String,
    val headshot: String?,
    val teamAbbr: String?,
    val teamLogo: String?,
    val teamId: String?,
    val value: String,
    val gamesPlayed: Int?,
)
