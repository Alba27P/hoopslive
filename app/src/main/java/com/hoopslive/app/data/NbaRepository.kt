package com.hoopslive.app.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * All data comes from ESPN's public (unofficial, key-less) JSON feeds:
 *  - site.api.espn.com      scoreboard, game summary/box score, standings, teams, rosters, schedules
 *  - site.web.api.espn.com  player season stats, game logs, league-wide stat leaders
 *  - sports.core.api.espn.com player bio, contracts, team season statistics
 */
object NbaRepository {
    private const val SITE = "https://site.api.espn.com/apis/site/v2/sports/basketball/nba"
    private const val SITE_V2 = "https://site.api.espn.com/apis/v2/sports/basketball/nba"
    private const val WEB = "https://site.web.api.espn.com/apis/common/v3/sports/basketball/nba"
    private const val CORE = "https://sports.core.api.espn.com/v2/sports/basketball/leagues/nba"

    private val YMD = DateTimeFormatter.BASIC_ISO_DATE
    val US_EASTERN: ZoneId = ZoneId.of("America/New_York")

    /** NBA game days are defined in US Eastern time. */
    fun todayGameDay(): LocalDate = LocalDate.now(US_EASTERN)

    /** ESPN labels a season by the year it ends in (2026-27 -> 2027). */
    fun currentSeasonYear(): Int {
        val d = todayGameDay()
        return if (d.monthValue >= 10) d.year + 1 else d.year
    }

    fun seasonLabel(year: Int): String = "${year - 1}-${(year % 100).toString().padStart(2, '0')}"

    // ---------------- Scores & games ----------------

    suspend fun scoreboard(day: LocalDate, force: Boolean = false): List<Game> {
        val url = "$SITE/scoreboard?dates=${day.format(YMD)}&limit=100"
        return EspnParsers.scoreboard(Http.getJson(url, if (force) 0 else 10_000))
            .sortedWith(compareBy<Game> { stateOrder(it.state) }.thenBy { it.date })
    }

    private fun stateOrder(s: GameState) = when (s) {
        GameState.LIVE -> 0
        GameState.PRE -> 1
        GameState.FINAL -> 2
        GameState.OTHER -> 3
    }

    suspend fun gameDetail(eventId: String, force: Boolean = false): GameDetail {
        val root = Http.getJson("$SITE/summary?event=$eventId", if (force) 0 else 8_000)
        return EspnParsers.gameDetail(root) ?: error("Game data not available")
    }

    // ---------------- Standings ----------------

    suspend fun standings(force: Boolean = false): List<Conference> =
        EspnParsers.standings(Http.getJson("$SITE_V2/standings", if (force) 0 else 5 * 60_000))

    // ---------------- Teams ----------------

    suspend fun teams(): List<TeamRef> =
        EspnParsers.teams(Http.getJson("$SITE/teams", 24 * 3_600_000L))

    suspend fun teamInfo(teamId: String): TeamInfo =
        EspnParsers.teamInfo(Http.getJson("$SITE/teams/$teamId", 5 * 60_000))

    suspend fun schedule(teamId: String, seasonType: Int): List<ScheduleGame> =
        EspnParsers.schedule(
            Http.getJson("$SITE/teams/$teamId/schedule?seasontype=$seasonType", 2 * 60_000),
            teamId,
        )

    suspend fun roster(teamId: String): List<RosterPlayer> =
        EspnParsers.roster(Http.getJson("$SITE/teams/$teamId/roster", 60 * 60_000))

    suspend fun teamStats(teamId: String): List<StatCategory> {
        val season = currentSeasonYear()
        val url = "$CORE/seasons/$season/types/2/teams/$teamId/statistics"
        return EspnParsers.statCategories(Http.getJson(url, 10 * 60_000))
    }

    // ---------------- Players ----------------

    data class PlayerProfile(
        val bio: PlayerBio,
        val team: TeamRef?,
        val college: String?,
        val contract: Contract?,
    )

    suspend fun playerProfile(athleteId: String, teamHint: String?): PlayerProfile = coroutineScope {
        val bio = EspnParsers.playerBio(Http.getJson("$CORE/athletes/$athleteId", 60 * 60_000))
        val teamId = teamHint?.takeIf { it.isNotBlank() } ?: bio.teamId
        val teamDeferred = async {
            teamId?.let { id -> runCatching { teams().firstOrNull { it.id == id } }.getOrNull() }
        }
        val collegeDeferred = async {
            bio.collegeRef?.let { ref -> runCatching { Http.getJson(ref, 24 * 3_600_000L).str("name") }.getOrNull() }
        }
        val rosterDeferred = async {
            teamId?.let { id -> runCatching { roster(id).firstOrNull { it.id == athleteId } }.getOrNull() }
        }
        val rosterEntry = rosterDeferred.await()
        val contract = rosterEntry?.contract ?: runCatching { coreContracts(bio) }.getOrNull()
        PlayerProfile(
            bio = bio,
            team = teamDeferred.await(),
            college = collegeDeferred.await() ?: rosterEntry?.college,
            contract = contract,
        )
    }

    /** Fallback for players not on a current roster: walk the core-API contract history. */
    private suspend fun coreContracts(bio: PlayerBio): Contract? = coroutineScope {
        val ref = bio.contractsRef ?: return@coroutineScope null
        val refs = Http.getJson(ref, 60 * 60_000).arr("items").objects()
            .mapNotNull { it.str("\$ref") }
            .take(8)
        val years = refs.map { r ->
            async { runCatching { EspnParsers.contractDoc(Http.getJson(r, 60 * 60_000), r) }.getOrNull() }
        }.mapNotNull { it.await() }
            .sortedByDescending { it.seasonYear }
        if (years.isEmpty()) null
        else Contract(
            salary = years.first().salary,
            yearsRemaining = null,
            seasonYear = years.first().seasonYear,
            history = years,
            tradeKicker = false,
        )
    }

    suspend fun playerStats(athleteId: String): List<StatTable> =
        EspnParsers.playerStats(Http.getJson("$WEB/athletes/$athleteId/stats", 10 * 60_000))

    suspend fun gameLog(athleteId: String): GameLog =
        EspnParsers.gameLog(Http.getJson("$WEB/athletes/$athleteId/gamelog", 5 * 60_000))

    // ---------------- League leaders ----------------

    val leaderCategories = listOf(
        LeaderCategory("pts", "Points", "offensive.avgPoints:desc", "offensive", "avgPoints"),
        LeaderCategory("reb", "Rebounds", "general.avgRebounds:desc", "general", "avgRebounds"),
        LeaderCategory("ast", "Assists", "offensive.avgAssists:desc", "offensive", "avgAssists"),
        LeaderCategory("stl", "Steals", "defensive.avgSteals:desc", "defensive", "avgSteals"),
        LeaderCategory("blk", "Blocks", "defensive.avgBlocks:desc", "defensive", "avgBlocks"),
        LeaderCategory("3pm", "3-Pointers Made", "offensive.avgThreePointFieldGoalsMade:desc", "offensive", "avgThreePointFieldGoalsMade"),
        LeaderCategory(
            "fgp", "Field Goal %", "offensive.fieldGoalPct:desc", "offensive", "fieldGoalPct",
            minStat = "offensive.avgFieldGoalsMade" to 3.65,
        ),
        LeaderCategory(
            "3pp", "3-Point %", "offensive.threePointFieldGoalPct:desc", "offensive", "threePointFieldGoalPct",
            minStat = "offensive.avgThreePointFieldGoalsMade" to 1.0,
        ),
        LeaderCategory(
            "ftp", "Free Throw %", "offensive.freeThrowPct:desc", "offensive", "freeThrowPct",
            minStat = "offensive.avgFreeThrowsMade" to 1.52,
        ),
        LeaderCategory("min", "Minutes", "general.avgMinutes:desc", "general", "avgMinutes"),
        LeaderCategory("to", "Turnovers", "offensive.avgTurnovers:desc", "offensive", "avgTurnovers"),
        LeaderCategory("dd2", "Double-Doubles", "general.doubleDouble:desc", "general", "doubleDouble", minGamesShare = 0.0),
        LeaderCategory("td3", "Triple-Doubles", "general.tripleDouble:desc", "general", "tripleDouble", minGamesShare = 0.0),
    )

    suspend fun leaders(cat: LeaderCategory, force: Boolean = false): List<LeaderEntry> {
        val season = currentSeasonYear()
        val limit = if (cat.minStat != null) 400 else 150
        val base = "$WEB/statistics/byathlete?region=us&lang=en&contentorigin=espn" +
            "&limit=$limit&sort=${cat.sort}&season=$season&seasontype=2"
        val age = if (force) 0L else 10 * 60_000L
        val qualified = runCatching {
            EspnParsers.leaders(Http.getJson("$base&isqualified=true", age), cat)
        }.getOrDefault(emptyList())
        if (qualified.isNotEmpty()) return qualified
        return EspnParsers.leaders(Http.getJson(base, age), cat)
    }
}
