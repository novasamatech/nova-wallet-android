package io.novafoundation.nova.core_db.model

import androidx.room.Embedded

class AssetWithToken(
    @Embedded
    val asset: AssetLocal?,

    @Embedded
    val token: TokenLocal?,

    @Embedded(prefix = "ca_")
    val assetAndChainId: AssetAndChainId,

    @Embedded
    val currency: CurrencyLocal
)

data class AssetAndChainId(
    val chainId: String,
    val assetId: Int
)
