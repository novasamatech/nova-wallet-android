package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import java.math.BigDecimal

@Entity(
    tableName = "coin_prices",
    primaryKeys = ["priceId", "currencyId", "timestamp"]
)
data class CoinPriceLocal(
    val priceId: String,
    val currencyId: String,
    val timestamp: Long,
    val rate: BigDecimal
)
