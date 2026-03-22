package edu.nd.pmcburne.hwapp.one.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

// data classes that match the JSON structure from the API

data class ScoreboardResponse(
    val games: List<GameWrapper>
)

data class GameWrapper(
    val game: GameData
)

data class GameData(
    val gameID: String,
    val gameState: String,
    val away: TeamData,
    val home: TeamData,
    val startTime: String,
    val startDate: String,
    val currentPeriod: String,
    val contestClock: String
)

data class TeamData(
    val score: String,
    val names: TeamNames,
    val winner: Boolean,
    val seed: String
)

data class TeamNames(
    val short: String,
    val char6: String
)

// the actual retrofit api interface
interface ScoreboardApi {
    @GET("scoreboard/basketball-{gender}/d1/{year}/{month}/{day}")
    suspend fun getScoreboard(
        @Path("gender") gender: String,
        @Path("year") year: String,
        @Path("month") month: String,
        @Path("day") day: String
    ): ScoreboardResponse
}

// creates a single retrofit instance we can reuse
object RetrofitClient {
    private const val BASE_URL = "https://ncaa-api.henrygd.me/"

    val api: ScoreboardApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ScoreboardApi::class.java)
    }
}
