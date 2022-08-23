package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "tokens")
data class TokenLocal(
    @PrimaryKey
    val symbol: String,
    val rate: BigDecimal?,
    val currencyId: Int,
    val recentRateChange: BigDecimal?,
) {
    companion object {
        fun createEmpty(symbol: String): TokenLocal = TokenLocal(symbol, null, 0, null)
    }

    enum class Type {
        KSM, DOT, WND, ROC
    }
}
