package com.hoopslive.app.data

import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Pure JSON -> model parsers for ESPN's public NBA feeds. Every field is read defensively:
 * the feeds are undocumented and fields come and go between game states and seasons.
 */
object EspnParsers {

    private val TEAM_REF_ID = Regex("/teams/(\\d+)")
    private val CONTRACT_YEAR = Regex("/contracts/(\\d{4})")

    fun parseInstant(s: String?): Instant? =
        s?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }

    fun team(t: JSONObject?): TeamRef {
        val logo = t.str("logo") ?: t.arr("logos").objects().firstOrNull()?.str("href")
        return TeamRef(
            id = t.str("id") ?: "",
            abbr = t.str("abbreviation") ?: "",
            name = t.str("displayName") ?: t.str("name") ?: "",
            shortName = t.str("shortDisplayName") ?: t.str("name") ?: t.str("abbreviation") ?: "",
            logo = logo,
            color = t.str("color"),
        )
    }

    private fun scoreOf(c: JSONObject): String? = when (val s = c.opt("score")) {
        is JSONObject -> s.str("displayValue")
        is String -> s.ifBlank { null }
        is Number -> s.toInt().toString()
        else -> null
    }

    fun state(type: JSONObject?): GameState = when (type.str("state")) {
        "pre" -> GameState.PRE
        "in" -> GameState.LIVE
        "post" -> GameState.FINAL
        else -> GameState.OTHER
    }

    private fun competitor(c: JSONObject): Competitor {
        val records = (c.arr("records") ?: c.arr("record")).objects()
        val record = (records.firstOrNull { it.str("type") == "total" } ?: records.firstOrNull())
            ?.let { it.str("summary") ?: it.str("displayValue") }
        return Competitor(
            team = team(c.obj("team")),
            score = scoreOf(c),
            record = record,
            isHome = c.str("homeAway") == "home",
            winner = c.bool("winner"),
            linescores = c.arr("linescores").strings(),
        )
    }

    private fun gameFromCompetition(id: String, comp: JSONObject, fallbackStatus: JSONObject?, fallbackDate: String?): Game? {
        val status = comp.obj("status") ?: fallbackStatus
        val type = status.obj("type")
        val comps = comp.arr("competitors").objects().map { competitor(it) }
        val home = comps.firstOrNull { it.isHome } ?: comps.getOrNull(0) ?: return null
        val away = comps.firstOrNull { !it.isHome && it !== home } ?: comps.firstOrNull { it !== home } ?: return null
        val broadcast = comp.arr("broadcasts").objects()
            .flatMap { b -> b.arr("names").strings() + listOfNotNull(b.obj("media").str("shortName")) }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
            .ifBlank { null }
        val headline = comp.arr("headlines").objects().firstOrNull()
            ?.let { it.str("shortLinkText") ?: it.str("description") }
        val seriesNote = (comp.opt("series") as? JSONObject)?.str("summary")
            ?: comp.arr("notes").objects().firstOrNull()?.str("headline")
        return Game(
            id = id,
            date = parseInstant(comp.str("date") ?: fallbackDate),
            state = state(type),
            statusText = type.str("shortDetail") ?: type.str("detail") ?: type.str("description") ?: "",
            away = away,
            home = home,
            broadcast = broadcast,
            headline = headline,
            seriesNote = seriesNote,
        )
    }

    fun game(e: JSONObject): Game? {
        val comp = e.arr("competitions").objects().firstOrNull() ?: return null
        return gameFromCompetition(e.str("id") ?: comp.str("id") ?: "", comp, e.obj("status"), e.str("date"))
    }

    fun scoreboard(root: JSONObject): List<Game> =
        root.arr("events").objects().mapNotNull { game(it) }

    // ---------------- Game summary ----------------

    fun gameDetail(root: JSONObject): GameDetail? {
        val header = root.obj("header")
        val comp = header.arr("competitions").objects().firstOrNull() ?: return null
        val game = gameFromCompetition(header.str("id") ?: comp.str("id") ?: "", comp, null, null) ?: return null
        val box = root.obj("boxscore")

        val boxes = box.arr("players").objects().map { p ->
            val st = p.arr("statistics").objects().firstOrNull()
            TeamBox(
                team = team(p.obj("team")),
                labels = st.arr("labels").strings(),
                players = st.arr("athletes").objects().map { a ->
                    val ath = a.obj("athlete")
                    val display = ath.str("displayName") ?: ""
                    BoxPlayer(
                        id = ath.str("id") ?: "",
                        name = display,
                        shortName = ath.str("shortName") ?: display,
                        position = ath.obj("position").str("abbreviation"),
                        jersey = ath.str("jersey"),
                        headshot = ath.obj("headshot").str("href"),
                        starter = a.bool("starter"),
                        didNotPlay = a.bool("didNotPlay"),
                        reason = a.str("reason"),
                        stats = a.arr("stats").strings(),
                    )
                },
                totals = st.arr("totals").strings(),
            )
        }.sortedBy { if (it.team.id == game.away.team.id) 0 else 1 }

        val teamBlocks = box.arr("teams").objects()
        val awayBlock = teamBlocks.firstOrNull { it.str("homeAway") == "away" }
            ?: teamBlocks.firstOrNull { it.obj("team").str("id") == game.away.team.id }
        val homeBlock = teamBlocks.firstOrNull { it.str("homeAway") == "home" }
            ?: teamBlocks.firstOrNull { it.obj("team").str("id") == game.home.team.id }
        val homeStats = homeBlock.arr("statistics").objects().associateBy { it.str("name") ?: "" }
        val teamStats = awayBlock.arr("statistics").objects().mapNotNull { s ->
            val name = s.str("name") ?: return@mapNotNull null
            TeamStatLine(
                label = s.str("label") ?: s.str("abbreviation") ?: name,
                away = s.str("displayValue") ?: "-",
                home = homeStats[name].str("displayValue") ?: "-",
            )
        }

        val plays = root.arr("plays").objects().map { p ->
            Play(
                id = p.str("id") ?: "",
                period = p.obj("period").int("number") ?: 0,
                periodLabel = p.obj("period").str("displayValue") ?: "",
                clock = p.obj("clock").str("displayValue") ?: "",
                text = p.str("text") ?: "",
                awayScore = p.int("awayScore") ?: 0,
                homeScore = p.int("homeScore") ?: 0,
                scoring = p.bool("scoringPlay"),
                teamId = p.obj("team").str("id"),
            )
        }

        val info = root.obj("gameInfo")
        val venueObj = info.obj("venue")
        val city = listOfNotNull(venueObj.obj("address").str("city"), venueObj.obj("address").str("state"))
            .joinToString(", ")
        val venue = venueObj.str("fullName")?.let { if (city.isNotBlank()) "$it · $city" else it }
        return GameDetail(
            game = game,
            boxes = boxes,
            teamStats = teamStats,
            plays = plays,
            venue = venue,
            attendance = info.int("attendance")?.takeIf { it > 0 },
            officials = info.arr("officials").objects().mapNotNull { it.str("displayName") ?: it.str("fullName") },
        )
    }

    // ---------------- Standings ----------------

    private fun standingRow(e: JSONObject): StandingRow {
        val stats = e.arr("stats").objects()
        fun byType(t: String): JSONObject? =
            stats.firstOrNull { it.str("type").equals(t, ignoreCase = true) }
                ?: stats.firstOrNull { it.str("name").equals(t, ignoreCase = true) }
        fun disp(t: String): String = byType(t)?.let { it.str("displayValue") ?: it.str("summary") } ?: "-"
        fun num(t: String): Double? = byType(t)?.let { it.dbl("value") ?: it.str("displayValue")?.toDoubleOrNull() }

        val clincher = byType("clincher")?.str("displayValue")?.takeIf { it.isNotBlank() && it != "-" }
        return StandingRow(
            team = team(e.obj("team")),
            seed = num("playoffseed")?.toInt() ?: 0,
            wins = num("wins")?.toInt() ?: 0,
            losses = num("losses")?.toInt() ?: 0,
            pct = disp("winpercent"),
            gb = disp("gamesbehind"),
            home = disp("home"),
            road = disp("road"),
            conf = disp("vsconf"),
            lastTen = disp("lasttengames"),
            streak = disp("streak"),
            ppg = disp("avgpointsfor"),
            oppPpg = disp("avgpointsagainst"),
            diff = disp("differential"),
            clincher = clincher,
            winPct = num("winpercent") ?: 0.0,
        )
    }

    fun standings(root: JSONObject): List<Conference> {
        val children = root.arr("children").objects()
        val groups = if (children.isNotEmpty()) children else listOf(root)
        return groups.mapNotNull { ch ->
            val entries = ch.obj("standings").arr("entries").objects()
            if (entries.isEmpty()) return@mapNotNull null
            val rows = entries.map { standingRow(it) }
                .sortedWith(
                    compareBy<StandingRow> { if (it.seed > 0) it.seed else 99 }
                        .thenByDescending { it.winPct }
                        .thenByDescending { it.wins }
                )
            Conference(ch.str("name") ?: "League", rows)
        }
    }

    // ---------------- Teams ----------------

    fun teams(root: JSONObject): List<TeamRef> =
        root.arr("sports").objects().firstOrNull()
            ?.arr("leagues").objects().firstOrNull()
            ?.arr("teams").objects().orEmpty()
            .map { team(it.obj("team")) }
            .filter { it.id.isNotBlank() }
            .sortedBy { it.name }

    fun teamInfo(root: JSONObject): TeamInfo {
        val t = root.obj("team")
        val items = t.obj("record").arr("items").objects()
        fun rec(type: String) = items.firstOrNull { it.str("type") == type }?.str("summary")
        return TeamInfo(
            team = team(t),
            record = rec("total") ?: items.firstOrNull()?.str("summary"),
            standingSummary = t.str("standingSummary"),
            homeRecord = rec("home"),
            roadRecord = rec("road"),
        )
    }

    fun schedule(root: JSONObject, teamId: String): List<ScheduleGame> =
        root.arr("events").objects().mapNotNull { e ->
            val comp = e.arr("competitions").objects().firstOrNull() ?: return@mapNotNull null
            val cs = comp.arr("competitors").objects()
            val self = cs.firstOrNull { it.str("id") == teamId || it.obj("team").str("id") == teamId }
                ?: return@mapNotNull null
            val opp = cs.firstOrNull { it !== self } ?: return@mapNotNull null
            val type = comp.obj("status").obj("type") ?: e.obj("status").obj("type")
            val st = state(type)
            ScheduleGame(
                id = e.str("id") ?: "",
                date = parseInstant(comp.str("date") ?: e.str("date")),
                opponent = team(opp.obj("team")),
                isHome = self.str("homeAway") == "home",
                state = st,
                statusText = type.str("shortDetail") ?: type.str("detail") ?: "",
                teamScore = scoreOf(self),
                oppScore = scoreOf(opp),
                won = if (st == GameState.FINAL && self.has("winner")) self.bool("winner") else null,
            )
        }.sortedBy { it.date ?: Instant.MAX }

    private fun contract(a: JSONObject): Contract? {
        val c = a.obj("contract")
        val history = a.arr("contracts").objects().mapNotNull { h ->
            val year = h.obj("season").int("year") ?: return@mapNotNull null
            val salary = h.long("salary") ?: return@mapNotNull null
            SalaryYear(year, salary)
        }.distinctBy { it.seasonYear }.sortedByDescending { it.seasonYear }
        if (c == null && history.isEmpty()) return null
        return Contract(
            salary = c.long("salary") ?: history.firstOrNull()?.salary,
            yearsRemaining = c.int("yearsRemaining"),
            seasonYear = c.obj("season").int("year") ?: history.firstOrNull()?.seasonYear,
            history = history,
            tradeKicker = c.obj("tradeKicker").bool("active"),
        )
    }

    fun roster(root: JSONObject): List<RosterPlayer> {
        val raw = root.arr("athletes").objects()
        // NBA rosters are flat; some sports group players under "items".
        val athletes = if (raw.firstOrNull()?.has("items") == true) raw.flatMap { it.arr("items").objects() } else raw
        return athletes.map { a ->
            RosterPlayer(
                id = a.str("id") ?: "",
                name = a.str("displayName") ?: a.str("fullName") ?: "",
                jersey = a.str("jersey"),
                position = a.obj("position").str("abbreviation"),
                age = a.int("age"),
                height = a.str("displayHeight"),
                weight = a.str("displayWeight"),
                headshot = a.obj("headshot").str("href"),
                experience = a.obj("experience").int("years"),
                college = a.obj("college").str("name"),
                injury = a.arr("injuries").objects().firstOrNull()?.str("status"),
                contract = contract(a),
            )
        }
    }

    /** Core-API statistics document (team or athlete): splits.categories[].stats[]. */
    fun statCategories(root: JSONObject): List<StatCategory> =
        root.obj("splits").arr("categories").objects().map { c ->
            StatCategory(
                name = c.str("name") ?: "",
                displayName = c.str("displayName") ?: c.str("name") ?: "",
                stats = c.arr("stats").objects().map { s ->
                    StatItem(
                        name = s.str("name") ?: "",
                        label = s.str("displayName") ?: s.str("name") ?: "",
                        abbr = s.str("abbreviation") ?: s.str("shortDisplayName") ?: "",
                        value = s.str("displayValue") ?: "-",
                        rank = s.str("rankDisplayValue"),
                    )
                },
            )
        }

    // ---------------- Players ----------------

    fun playerBio(a: JSONObject): PlayerBio {
        val bp = a.obj("birthPlace")
        val birthPlace = listOfNotNull(bp.str("city"), bp.str("state"), bp.str("country"))
            .joinToString(", ").ifBlank { null }
        val birthDate = parseInstant(a.str("dateOfBirth"))?.atOffset(ZoneOffset.UTC)?.toLocalDate()?.toString()
        return PlayerBio(
            id = a.str("id") ?: "",
            name = a.str("displayName") ?: a.str("fullName") ?: "",
            headshot = a.obj("headshot").str("href"),
            position = a.obj("position").str("displayName") ?: a.obj("position").str("abbreviation"),
            jersey = a.str("jersey"),
            age = a.int("age"),
            birthDate = birthDate,
            birthPlace = birthPlace,
            height = a.str("displayHeight"),
            weight = a.str("displayWeight"),
            experience = a.obj("experience").int("years"),
            draft = a.obj("draft").str("displayText"),
            teamId = a.obj("team").str("\$ref")?.let { TEAM_REF_ID.find(it)?.groupValues?.get(1) },
            collegeRef = a.obj("college").str("\$ref"),
            contractsRef = a.obj("contracts").str("\$ref"),
            active = a.bool("active"),
            hand = a.obj("hand").str("displayValue"),
        )
    }

    fun playerStats(root: JSONObject): List<StatTable> {
        val teamAbbr = HashMap<String, String>()
        root.obj("teams")?.let { teams ->
            for (k in teams.keyList()) {
                val t = teams.optJSONObject(k) ?: continue
                val abbr = t.str("abbreviation") ?: continue
                t.str("id")?.let { teamAbbr[it] = abbr }
                teamAbbr[k] = abbr
            }
        }
        return root.arr("categories").objects().map { c ->
            StatTable(
                name = c.str("name") ?: "",
                title = c.str("displayName") ?: c.str("name") ?: "",
                labels = c.arr("labels").strings(),
                rows = c.arr("statistics").objects().map { s ->
                    val season = s.obj("season")
                    SeasonRow(
                        season = season.str("displayName") ?: season.str("year") ?: "",
                        year = season.int("year") ?: 0,
                        team = s.str("teamSlug")?.let { teamAbbr[it] } ?: s.str("teamId")?.let { teamAbbr[it] },
                        stats = s.arr("stats").strings(),
                    )
                }.sortedByDescending { it.year },
                totals = c.arr("totals").strings(),
            )
        }
    }

    fun gameLog(root: JSONObject): GameLog {
        val labels = root.arr("labels").strings()
        val meta = root.obj("events")
        val rows = ArrayList<GameLogRow>()
        val seen = HashSet<String>()
        for (st in root.arr("seasonTypes").objects()) {
            if (st.str("displayName")?.contains("Preseason", ignoreCase = true) == true) continue
            for (cat in st.arr("categories").objects()) {
                if (cat.str("type") != null && cat.str("type") != "event") continue
                for (ev in cat.arr("events").objects()) {
                    val id = ev.str("eventId") ?: continue
                    if (!seen.add(id)) continue
                    val m = meta.obj(id)
                    rows.add(
                        GameLogRow(
                            eventId = id,
                            date = parseInstant(m.str("gameDate")),
                            atVs = m.str("atVs") ?: "",
                            opponent = m.obj("opponent").str("abbreviation") ?: "",
                            opponentLogo = m.obj("opponent").str("logo"),
                            result = m.str("gameResult"),
                            score = m.str("score"),
                            stats = ev.arr("stats").strings(),
                        )
                    )
                }
            }
        }
        rows.sortByDescending { it.date ?: Instant.MIN }
        return GameLog(labels, rows)
    }

    fun contractDoc(doc: JSONObject, refUrl: String): SalaryYear? {
        val salary = doc.long("salary") ?: return null
        val year = doc.obj("season").int("year")
            ?: doc.int("season")
            ?: CONTRACT_YEAR.find(refUrl)?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        return SalaryYear(year, salary)
    }

    // ---------------- Leaders ----------------

    fun leaders(root: JSONObject, cat: LeaderCategory, max: Int = 50): List<LeaderEntry> {
        val names: Map<String, List<String>> = root.arr("categories").objects()
            .associate { (it.str("name") ?: "") to it.arr("names").strings() }

        class Row(val a: JSONObject, val cats: Map<String, JSONObject>) {
            fun idx(category: String, stat: String) = names[category]?.indexOf(stat) ?: -1
            fun value(category: String, stat: String): Double? {
                val i = idx(category, stat)
                return if (i < 0) null else cats[category].arr("values").doubles().getOrNull(i)
            }
            fun display(category: String, stat: String): String? {
                val i = idx(category, stat)
                return if (i < 0) null else cats[category].arr("totals").strings().getOrNull(i)
            }
        }

        val rows = root.arr("athletes").objects().map { a ->
            Row(a, a.arr("categories").objects().associateBy { it.str("name") ?: "" })
        }.filter { it.value(cat.category, cat.stat) != null }

        val maxGp = rows.mapNotNull { it.value("general", "gamesPlayed") }.maxOrNull() ?: 0.0
        val qualified = rows.filter { r ->
            val gp = r.value("general", "gamesPlayed") ?: 0.0
            val gpOk = cat.minGamesShare <= 0.0 || gp >= maxGp * cat.minGamesShare
            val minOk = cat.minStat?.let { (key, min) ->
                val parts = key.split('.')
                (r.value(parts[0], parts.getOrElse(1) { "" }) ?: 0.0) >= min
            } ?: true
            gpOk && minOk
        }.sortedByDescending { it.value(cat.category, cat.stat) ?: Double.NEGATIVE_INFINITY }
            .take(max)

        return qualified.mapIndexed { i, r ->
            val ath = r.a.obj("athlete")
            val v = r.value(cat.category, cat.stat)
            LeaderEntry(
                rank = i + 1,
                athleteId = ath.str("id") ?: "",
                name = ath.str("displayName") ?: "",
                headshot = ath.obj("headshot").str("href"),
                teamAbbr = ath.str("teamShortName") ?: ath.arr("teams").objects().firstOrNull()?.str("abbreviation"),
                teamLogo = ath.arr("teamLogos").objects().firstOrNull()?.str("href"),
                teamId = ath.str("teamId"),
                value = r.display(cat.category, cat.stat) ?: v?.let { String.format("%.1f", it) } ?: "-",
                gamesPlayed = r.value("general", "gamesPlayed")?.toInt(),
            )
        }
    }
}
