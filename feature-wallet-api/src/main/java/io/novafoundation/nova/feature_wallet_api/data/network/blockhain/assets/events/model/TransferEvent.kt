package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.model

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.runtime.AccountId

class TransferEvent(
    val from: AccountId,
    val to: AccountId,
    val amount: Balance
)
