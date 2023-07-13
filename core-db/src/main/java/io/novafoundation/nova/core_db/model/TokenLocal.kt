package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import io.novafoundation.nova.common.utils.Identifiable
import java.math.BigDecimal

@Entity(tableName = "tokens", primaryKeys = ["tokenSymbol", "currencyId"])
data class TokenLocal(
    val tokenSymbol: String,
    val rate: BigDecimal?,
    val currencyId: Int,
    val recentRateChange: BigDecimal?,
) : Identifiable {
    companion object {
        fun createEmpty(symbol: String, currencyId: Int): TokenLocal = TokenLocal(symbol, null, currencyId, null)
    }

    override val identifier: String
        get() = "$tokenSymbol:$currencyId"
}
