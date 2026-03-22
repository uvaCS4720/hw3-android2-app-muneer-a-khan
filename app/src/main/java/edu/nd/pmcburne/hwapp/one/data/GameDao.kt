package edu.nd.pmcburne.hwapp.one.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GameDao {
    // get all games for a specific date and gender
    @Query("SELECT * FROM games WHERE date = :date AND gender = :gender")
    suspend fun getGames(date: String, gender: String): List<GameEntity>

    // insert games into the db, replace if a game with same id already exists
    // this handles updating scores when we refresh
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<GameEntity>)

    // delete all games for a specific date and gender
    // useful if we want to do a clean refresh
    @Query("DELETE FROM games WHERE date = :date AND gender = :gender")
    suspend fun deleteGames(date: String, gender: String)
}
