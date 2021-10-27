package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.TokenLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TokenDao {

    @Query("select * from tokens where symbol = :symbol")
    abstract suspend fun getToken(symbol: String): TokenLocal?

    @Query("select * from tokens where symbol = :symbol")
    abstract fun observeToken(symbol: String): Flow<TokenLocal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertToken(token: TokenLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTokens(tokens: List<TokenLocal>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTokenOrIgnore(token: TokenLocal)

    suspend fun ensureToken(symbol: String) = insertTokenOrIgnore(TokenLocal.createEmpty(symbol))
}
