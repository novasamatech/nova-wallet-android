package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core_db.model.TokenLocal
import io.novafoundation.nova.core_db.model.TokenWithCurrency
import kotlinx.coroutines.flow.Flow

private const val RETRIEVE_TOKEN_WITH_CURRENCY = """
    SELECT * FROM currencies AS currency
    LEFT OUTER JOIN tokens AS token ON token.currencyId = currency.id AND token.tokenSymbol = :symbol
    WHERE currency.selected = 1
"""

private const val RETRIEVE_TOKENS_WITH_CURRENCY = """
    SELECT * FROM currencies AS currency
    LEFT OUTER JOIN tokens AS token ON token.currencyId = currency.id AND token.tokenSymbol in (:symbols)
    WHERE currency.selected = 1
"""

private const val INSERT_TOKEN_WITH_SELECTED_CURRENCY = """
    INSERT OR IGNORE INTO tokens (tokenSymbol, rate, currencyId, recentRateChange)
    VALUES(:symbol, NULL, (SELECT id FROM currencies WHERE selected = 1), NULL)
"""

@Dao
abstract class TokenDao {

    @Transaction
    open suspend fun applyDiff(diff: CollectionDiffer.Diff<TokenLocal>) {
        deleteTokens(diff.removed)
        insertTokens(diff.added)
        updateTokens(diff.updated)
    }

    @Query(RETRIEVE_TOKEN_WITH_CURRENCY)
    abstract suspend fun getTokenWithCurrency(symbol: String): TokenWithCurrency?

    @Query(RETRIEVE_TOKENS_WITH_CURRENCY)
    abstract fun observeTokensWithCurrency(symbols: List<String>): Flow<List<TokenWithCurrency>>

    @Query(RETRIEVE_TOKENS_WITH_CURRENCY)
    abstract fun getTokensWithCurrency(symbols: List<String>): List<TokenWithCurrency>

    @Query(RETRIEVE_TOKEN_WITH_CURRENCY)
    abstract fun observeTokenWithCurrency(symbol: String): Flow<TokenWithCurrency>

    @Query("SELECT * FROM tokens")
    abstract suspend fun getTokens(): List<TokenLocal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTokens(tokens: List<TokenLocal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertToken(token: TokenLocal)

    @Query(INSERT_TOKEN_WITH_SELECTED_CURRENCY)
    abstract suspend fun insertTokenWithSelectedCurrency(symbol: String)

    @Update
    abstract suspend fun updateTokens(chains: List<TokenLocal>)

    @Delete
    abstract suspend fun deleteTokens(tokens: List<TokenLocal>)

    @Query("DELETE FROM tokens")
    abstract suspend fun deleteAll()
}
