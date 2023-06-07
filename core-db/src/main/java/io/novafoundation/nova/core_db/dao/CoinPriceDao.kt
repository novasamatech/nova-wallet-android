package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.CoinPriceLocal

@Dao
interface CoinPriceDao {

    @Query(
        """
        SELECT * FROM coin_prices 
        WHERE priceId = :priceId AND currencyId = :currencyId
        AND timestamp >= :timestamp
        ORDER BY timestamp DESC LIMIT 1
        """
    )
    suspend fun getCoinPriceAtTime(priceId: String, currencyId: String, timestamp: Long): CoinPriceLocal?

    @Query(
        """
        SELECT * FROM coin_prices 
        WHERE priceId = :priceId AND currencyId = :currencyId
        AND timestamp BETWEEN :fromTimestamp AND :toTimestamp
        """
    )
    suspend fun getCoinPriceRange(priceId: String, currencyId: String, fromTimestamp: Long, toTimestamp: Long): List<CoinPriceLocal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCoinPrices(coinPrices: List<CoinPriceLocal>)

    @Query(
        """
        DELETE FROM coin_prices 
        WHERE priceId = :priceId AND currencyId = :currencyId
        """
    )
    fun deleteCoinPrices(priceId: String, currencyId: String)
}
