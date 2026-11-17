# Hoops Live — NBA live scores for Android

Native Android app (Kotlin + Jetpack Compose, Material 3) in the spirit of Sofascore: live scores,
box scores, standings, teams, player profiles with contracts, and league leaders.

## Features

| Tab | What's there |
|---|---|
| **Scores** | Date strip (±45 days), live games first with pulsing LIVE badge, auto-refresh every 20 s while games are live. Tip-off times shown in your phone's time zone. |
| **Game** | Big scoreboard, quarter-by-quarter line score, **full box score** for both teams (starters/bench/DNP, frozen name column, scroll stats sideways), **team stats** comparison bars, **play-by-play**, arena/attendance/officials. Refreshes every 15 s while live. |
| **Standings** | East / West / whole league, with W-L, PCT, GB, home/away, conference, L10, streak, PPG, OPP PPG, differential. Playoff (1–6) and play-in (7–10) markers. |
| **Leaders** | Points, rebounds, assists, steals, blocks, 3PM, FG%, 3P%, FT%, minutes, turnovers, double-doubles, triple-doubles. Rate stats use NBA-style qualifying minimums. |
| **Teams** | All 30 teams → record & standing, **schedule** (regular season / playoffs, results and upcoming), **roster with salaries & years left** (plus total payroll), **team season stats with league ranks**. |
| **Player** | Bio (age, born, height/weight, draft, college, experience), current-season averages, **career stats** (averages/totals/misc per season), **game log**, **contract** (current salary, years remaining, salary history by season). |

Tap any player, team or game anywhere to drill in.

## Get the APK

### Option A — GitHub Actions (no Android tooling needed)
1. Create a new GitHub repository and push this folder to it.
2. GitHub runs **Actions → Build APK** automatically (≈5 min).
3. Open the finished run → **Artifacts → HoopsLive-apk** → download and unzip.
   - `HoopsLive.apk` — optimized release build (≈3–5 MB)
   - `HoopsLive-debug.apk` — fallback build, always produced
4. Copy to your phone and open it (allow "install unknown apps" for your file manager/browser).

Push a tag like `v1.0.0` to also get the APK attached to a GitHub Release.

### Option B — Android Studio
Open the folder in Android Studio (Ladybug or newer) → **Build → Build APK(s)**,
or from a terminal with the Android SDK installed: `./gradlew assembleRelease`.

## Data source

Uses ESPN's public, key-less JSON feeds (the same ones espn.com uses):

- `site.api.espn.com` — scoreboard, game summary/box score/play-by-play, standings, teams, rosters (incl. contracts), schedules
- `site.web.api.espn.com` — player season stats, game logs, league-wide stat leaders
- `sports.core.api.espn.com` — player bio, contract history, team season statistics with ranks

These feeds are unofficial and undocumented: they're fine for personal use, but ESPN can change them
without notice, and you shouldn't publish an app built on them to the Play Store without checking
ESPN's terms. All network/parsing code lives in `app/src/main/java/com/hoopslive/app/data/`,
so swapping in a licensed provider (e.g. Sportradar, balldontlie) only touches that package.

## Project layout

```
app/src/main/java/com/hoopslive/app/
├── MainActivity.kt
├── data/            Http client + cache, ESPN parsers, repository, models
└── ui/
    ├── AppNav.kt    bottom navigation + routes
    ├── Loader.kt    lifecycle-aware loading/polling ViewModel
    ├── components/  logos, headshots, frozen-column stat table, etc.
    ├── screens/     Scores, Game, Standings, Leaders, Teams, Team, Player
    └── theme/       dark/light Material 3 theme
```

Tech: Kotlin 2.1, AGP 8.7, Compose BOM 2024.12, Navigation Compose, Coil, coroutines.
minSdk 26 (Android 8.0), targetSdk 35. The release build is signed with the debug key so it
sideloads directly; set up your own keystore before distributing it.
