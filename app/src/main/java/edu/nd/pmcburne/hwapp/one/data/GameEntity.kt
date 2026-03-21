package edu.nd.pmcburne.hwapp.one.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// this stores one game in the local database
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey
    val gameId: String,

    // used to query games by date and gender
    val date: String,
    val gender: String,

    // team names
    val homeTeam: String,
    val awayTeam: String,

    // scores (empty string if the game hasnt started)
    val homeScore: String,
    val awayScore: String,

    // "pre", "in", or "final"
    val gameState: String,

    // time the game starts like "6:40 PM ET"
    val startTime: String,

    // stuff like "1st Half", "2nd Half", "FINAL", etc
    val currentPeriod: String,

    // time left on the clock like "17:30"
    val contestClock: String,

    // which team won (only matters when gameState is "final")
    val homeWinner: Boolean,
    val awayWinner: Boolean,

    // seed number if its a tournament game (can be empty)
    val homeSeed: String,
    val awaySeed: String
)
