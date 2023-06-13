package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.novafoundation.nova.core_db.model.CoinPriceLocal

@Dao
interface CoinPriceDao {

    @Query(
        """
        SELECT * FROM coin_prices 
        WHERE priceId = :priceId AND currencyId = :currencyId
        AND timestamp <= :timestamp
        ORDER BY timestamp DESC LIMIT 1
        """
    )
    suspend fun getFloorCoinPriceAtTime(priceId: String, currencyId: String, timestamp: Long): CoinPriceLocal?

    @Query(
        """
        SELECT EXISTS(
            SELECT * FROM coin_prices 
            WHERE priceId = :priceId AND currencyId = :currencyId
            AND timestamp >= :timestamp
            ORDER BY timestamp ASC LIMIT 1
        )
        """
    )
    suspend fun hasCeilingCoinPriceAtTime(priceId: String, currencyId: String, timestamp: Long): Boolean

    @Query(
        """
        SELECT * FROM coin_prices 
        WHERE priceId = :priceId AND currencyId = :currencyId
        AND timestamp BETWEEN :fromTimestamp AND :toTimestamp
        ORDER BY timestamp ASC
        """
    )
    suspend fun getCoinPriceRange(priceId: String, currencyId: String, fromTimestamp: Long, toTimestamp: Long): List<CoinPriceLocal>

    @Transaction
    suspend fun updateCoinPrices(priceId: String, currencyId: String, coinRates: List<CoinPriceLocal>) {
        deleteCoinPrices(priceId, currencyId)
        setCoinPrices(coinRates)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCoinPrices(coinPrices: List<CoinPriceLocal>)

    @Query(
        """
        DELETE FROM coin_prices 
        WHERE priceId = :priceId AND currencyId = :currencyId
        """
    )
    fun deleteCoinPrices(priceId: String, currencyId: String)
}
