package com.hoopslive.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hoopslive.app.ui.screens.GameScreen
import com.hoopslive.app.ui.screens.LeadersScreen
import com.hoopslive.app.ui.screens.PlayerScreen
import com.hoopslive.app.ui.screens.ScoresScreen
import com.hoopslive.app.ui.screens.StandingsScreen
import com.hoopslive.app.ui.screens.TeamScreen
import com.hoopslive.app.ui.screens.TeamsScreen

private data class TopDest(val route: String, val label: String, val icon: ImageVector)

private val TOP = listOf(
    TopDest("scores", "Scores", Icons.Filled.DateRange),
    TopDest("standings", "Standings", Icons.AutoMirrored.Filled.List),
    TopDest("leaders", "Leaders", Icons.Filled.Star),
    TopDest("teams", "Teams", Icons.Filled.AccountBox),
)

private fun NavHostController.openGame(id: String) = navigate("game/${Uri.encode(id)}")
private fun NavHostController.openTeam(id: String) = navigate("team/${Uri.encode(id)}")
private fun NavHostController.openPlayer(id: String, teamId: String?) =
    navigate("player/${Uri.encode(id)}?team=${Uri.encode(teamId ?: "")}")

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = TOP.any { it.route == route }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    TOP.forEach { d ->
                        NavigationBarItem(
                            selected = route == d.route,
                            onClick = {
                                nav.navigate(d.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(d.icon, contentDescription = d.label) },
                            label = { Text(d.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "scores", modifier = Modifier.padding(padding)) {
            composable("scores") { ScoresScreen(onGame = nav::openGame) }
            composable("standings") { StandingsScreen(onTeam = nav::openTeam) }
            composable("leaders") { LeadersScreen(onPlayer = nav::openPlayer) }
            composable("teams") { TeamsScreen(onTeam = nav::openTeam) }
            composable(
                "game/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { e ->
                GameScreen(
                    gameId = e.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onPlayer = nav::openPlayer,
                    onTeam = nav::openTeam,
                )
            }
            composable(
                "team/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { e ->
                TeamScreen(
                    teamId = e.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onGame = nav::openGame,
                    onPlayer = nav::openPlayer,
                )
            }
            composable(
                "player/{id}?team={team}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("team") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { e ->
                PlayerScreen(
                    playerId = e.arguments?.getString("id").orEmpty(),
                    teamHint = e.arguments?.getString("team")?.takeIf { it.isNotBlank() },
                    onBack = { nav.popBackStack() },
                    onTeam = nav::openTeam,
                    onGame = nav::openGame,
                )
            }
        }
    }
}
