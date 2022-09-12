package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import java.math.BigDecimal

@Entity(tableName = "tokens", primaryKeys = ["tokenSymbol", "currencyId"])
data class TokenLocal(
    val tokenSymbol: String,
    val rate: BigDecimal?,
    val currencyId: Int,
    val recentRateChange: BigDecimal?,
) {
    companion object {
        fun createEmpty(symbol: String, currencyId: Int): TokenLocal = TokenLocal(symbol, null, currencyId, null)
    }
}
