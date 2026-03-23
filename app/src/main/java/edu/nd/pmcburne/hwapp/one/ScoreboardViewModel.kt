package edu.nd.pmcburne.hwapp.one

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import edu.nd.pmcburne.hwapp.one.data.GameDao
import edu.nd.pmcburne.hwapp.one.data.GameEntity
import edu.nd.pmcburne.hwapp.one.data.RetrofitClient
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ScoreboardViewModel(private val dao: GameDao) : ViewModel() {

    // the date we're looking at, starts as today
    var selectedDate by mutableStateOf(LocalDate.now())
        private set

    // "men" or "women"
    var gender by mutableStateOf("men")
        private set

    // list of games to show
    var games by mutableStateOf<List<GameEntity>>(emptyList())
        private set

    // loading spinner state
    var isLoading by mutableStateOf(false)
        private set

    // error message if something goes wrong
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // whether we're showing cached data (offline)
    var isOffline by mutableStateOf(false)
        private set

    init {
        loadGames()
    }

    // call this whenever date, gender changes, or user refreshes
    fun loadGames() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            isOffline = false

            val dateStr = selectedDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
            val year = selectedDate.format(DateTimeFormatter.ofPattern("yyyy"))
            val month = selectedDate.format(DateTimeFormatter.ofPattern("MM"))
            val day = selectedDate.format(DateTimeFormatter.ofPattern("dd"))

            try {
                // try to get data from the api
                val response = RetrofitClient.api.getScoreboard(gender, year, month, day)

                // convert api response to entities
                val entities = response.games.map { wrapper ->
                    val g = wrapper.game
                    GameEntity(
                        gameId = g.gameID,
                        date = dateStr,
                        gender = gender,
                        homeTeam = g.home.names.short,
                        awayTeam = g.away.names.short,
                        homeScore = g.home.score,
                        awayScore = g.away.score,
                        gameState = g.gameState,
                        startTime = g.startTime,
                        currentPeriod = g.currentPeriod,
                        contestClock = g.contestClock,
                        homeWinner = g.home.winner,
                        awayWinner = g.away.winner,
                        homeSeed = g.home.seed,
                        awaySeed = g.away.seed
                    )
                }

                // save to database
                dao.insertAll(entities)

                // update ui
                games = entities

            } catch (e: Exception) {
                // if api fails (no internet, etc), load from database instead
                val cached = dao.getGames(dateStr, gender)
                if (cached.isNotEmpty()) {
                    games = cached
                    isOffline = true
                    errorMessage = "Showing cached data (offline)"
                } else {
                    games = emptyList()
                    errorMessage = "No internet and no cached data for this date"
                }
            }

            isLoading = false
        }
    }

    fun onDateSelected(newDate: LocalDate) {
        selectedDate = newDate
        loadGames()
    }

    fun onGenderToggle() {
        gender = if (gender == "men") "women" else "men"
        loadGames()
    }
}

// factory so we can pass the dao into the viewmodel
class ScoreboardViewModelFactory(private val dao: GameDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScoreboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScoreboardViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
