package io.novafoundation.nova.feature_assets.presentation.model

import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class BalanceLocksModel(
    val locks: List<Lock>
) {

    class Lock(
        val name: String,
        val amount: AmountModel
    )
}
