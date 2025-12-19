package io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class RedeemValidationPayload(
    val fee: Fee,
    val asset: Asset,
    val chain: Chain
)
