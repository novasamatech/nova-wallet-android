package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount

class TransferParsedFromCall(
    val amount: ChainAssetWithAmount,
    val destination: AccountIdKey
)
