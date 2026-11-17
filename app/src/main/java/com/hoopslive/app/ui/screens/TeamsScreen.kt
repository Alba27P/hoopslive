package com.hoopslive.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoopslive.app.data.NbaRepository
import com.hoopslive.app.data.TeamRef
import com.hoopslive.app.ui.components.AppTopBar
import com.hoopslive.app.ui.components.LoadContent
import com.hoopslive.app.ui.components.TeamLogo
import com.hoopslive.app.ui.rememberLoad

@Composable
fun TeamsScreen(onTeam: (String) -> Unit) {
    val h = rememberLoad<List<TeamRef>>(key = "teams") { NbaRepository.teams() }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "Teams", subtitle = "Rosters, contracts, schedules & stats")
        LoadContent(h) { teams ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(104.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(teams) { t ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable { onTeam(t.id) }
                            .padding(vertical = 14.dp, horizontal = 6.dp),
                    ) {
                        TeamLogo(t.logo, 52.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            t.shortName,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            t.abbr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
