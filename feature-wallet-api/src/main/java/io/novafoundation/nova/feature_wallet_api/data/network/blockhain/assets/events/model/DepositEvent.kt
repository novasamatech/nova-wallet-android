package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class DepositEvent(
    val destination: AccountIdKey,
    val amount: Balance,
)
