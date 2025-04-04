package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model

import io.novafoundation.nova.common.address.AccountIdKey
import java.math.BigInteger

class StatemineAssetDetails(
    val status: Status,
    val isSufficient: Boolean,
    val minimumBalance: BigInteger,
    val issuer: AccountIdKey
) {

    enum class Status {
        Live, Frozen, Destroying
    }
}

val StatemineAssetDetails.Status.transfersFrozen: Boolean
    get() = this != StatemineAssetDetails.Status.Live
