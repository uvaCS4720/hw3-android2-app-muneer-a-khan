package edu.nd.pmcburne.hwapp.one

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.nd.pmcburne.hwapp.one.data.AppDatabase
import edu.nd.pmcburne.hwapp.one.data.GameEntity
import edu.nd.pmcburne.hwapp.one.ui.theme.HWStarterRepoTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.gameDao()

        enableEdgeToEdge()
        setContent {
            HWStarterRepoTheme {
                val viewModel: ScoreboardViewModel = viewModel(
                    factory = ScoreboardViewModelFactory(dao)
                )
                ScoreboardScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreboardScreen(viewModel: ScoreboardViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NCAA Basketball Scores") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // date picker button and gender toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // date button
                Button(onClick = { showDatePicker = true }) {
                    val formatted = viewModel.selectedDate.format(
                        DateTimeFormatter.ofPattern("MMM d, yyyy")
                    )
                    Text(formatted)
                }

                // gender toggle chips
                Row {
                    FilterChip(
                        selected = viewModel.gender == "men",
                        onClick = {
                            if (viewModel.gender != "men") viewModel.onGenderToggle()
                        },
                        label = { Text("Men") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = viewModel.gender == "women",
                        onClick = {
                            if (viewModel.gender != "women") viewModel.onGenderToggle()
                        },
                        label = { Text("Women") }
                    )
                }
            }

            // show error/offline message if there is one
            if (viewModel.errorMessage != null) {
                Text(
                    text = viewModel.errorMessage!!,
                    color = if (viewModel.isOffline)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic
                )
            }

            // loading indicator
            if (viewModel.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // game list with pull to refresh
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh = { viewModel.loadGames() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (viewModel.games.isEmpty() && !viewModel.isLoading) {
                    // no games message
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No games found for this date",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // little spacer at top
                        item { Spacer(modifier = Modifier.height(4.dp)) }

                        items(viewModel.games) { game ->
                            GameCard(game = game, gender = viewModel.gender)
                        }

                        // spacer at bottom so last card isnt cut off
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }

    // date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            viewModel.onDateSelected(picked)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun GameCard(game: GameEntity, gender: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // status line at the top
            Text(
                text = getStatusText(game, gender),
                style = MaterialTheme.typography.labelMedium,
                color = getStatusColor(game),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // away team row
            TeamRow(
                teamName = game.awayTeam,
                seed = game.awaySeed,
                score = game.awayScore,
                isWinner = game.awayWinner,
                showScore = game.gameState != "pre",
                label = "AWAY"
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // home team row
            TeamRow(
                teamName = game.homeTeam,
                seed = game.homeSeed,
                score = game.homeScore,
                isWinner = game.homeWinner,
                showScore = game.gameState != "pre",
                label = "HOME"
            )
        }
    }
}

@Composable
fun TeamRow(
    teamName: String,
    seed: String,
    score: String,
    isWinner: Boolean,
    showScore: Boolean,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // team name with seed and home/away label
        Row(verticalAlignment = Alignment.CenterVertically) {
            // show seed if its a tournament game
            if (seed.isNotEmpty()) {
                Text(
                    text = "($seed) ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column {
                Text(
                    text = teamName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // score
        if (showScore) {
            Text(
                text = score,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.End
            )
        }
    }
}

// figures out what to show for the status text
fun getStatusText(game: GameEntity, gender: String): String {
    return when (game.gameState) {
        "pre" -> game.startTime
        "final" -> "Final"
        "in" -> {
            // show period and clock
            val period = game.currentPeriod
            val clock = game.contestClock
            if (period.isNotEmpty() && clock.isNotEmpty()) {
                "$period - $clock"
            } else if (period.isNotEmpty()) {
                period
            } else {
                "In Progress"
            }
        }
        else -> game.gameState
    }
}

// returns a color based on game state
@Composable
fun getStatusColor(game: GameEntity): androidx.compose.ui.graphics.Color {
    return when (game.gameState) {
        "pre" -> MaterialTheme.colorScheme.primary
        "in" -> MaterialTheme.colorScheme.error
        "final" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
}
