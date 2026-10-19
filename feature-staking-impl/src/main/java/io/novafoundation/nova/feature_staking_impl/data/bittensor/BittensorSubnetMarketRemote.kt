package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class BittensorSubnetMarketRemote(
    val id: String?,
    val symbol: String?,
    @SerializedName("price_change_percentage_7d_in_currency") val weekChangePercent: BigDecimal? = null,
    @SerializedName("last_updated") val lastUpdated: String? = null,
    @SerializedName("sparkline_in_7d") val weeklySparkline: BittensorMarketSparklineRemote? = null
)

data class BittensorMarketSparklineRemote(val price: List<BigDecimal?>?)
