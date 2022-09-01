package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.TokenLocal
import io.novafoundation.nova.core_db.model.TokenWithCurrency
import kotlinx.coroutines.flow.Flow

private const val RETRIEVE_TOKEN_WITH_CURRENCY = """
    SELECT * FROM currencies AS currency
    LEFT OUTER JOIN tokens AS token ON token.currencyId = currency.id
    WHERE token.tokenSymbol = :symbol AND currency.selected = 1
"""

private const val RETRIEVE_TOKENS_WITH_CURRENCY = """
    SELECT * FROM currencies AS currency
    LEFT OUTER JOIN tokens AS token ON token.currencyId = currency.id
    WHERE token.tokenSymbol in (:symbols) AND currency.selected = 1
"""

private const val INSERT_TOKEN_WITH_SELECTED_CURRENCY = """
    INSERT OR IGNORE INTO tokens (tokenSymbol, rate, currencyId, recentRateChange)
    VALUES(:symbol, NULL, (SELECT id FROM currencies WHERE selected = 1), NULL)
"""

@Dao
abstract class TokenDao {

    @Query(RETRIEVE_TOKEN_WITH_CURRENCY)
    abstract suspend fun getToken(symbol: String): TokenWithCurrency

    @Query(RETRIEVE_TOKENS_WITH_CURRENCY)
    abstract fun observeTokens(symbols: List<String>): Flow<List<TokenWithCurrency>>

    @Query(RETRIEVE_TOKEN_WITH_CURRENCY)
    abstract fun observeToken(symbol: String): Flow<TokenWithCurrency>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTokens(tokens: List<TokenLocal>)

    @Query(INSERT_TOKEN_WITH_SELECTED_CURRENCY)
    abstract suspend fun insertTokenWithSelectedCurrency(symbol: String)
}
