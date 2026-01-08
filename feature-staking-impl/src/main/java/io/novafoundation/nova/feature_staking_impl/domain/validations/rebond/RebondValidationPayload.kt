package io.novafoundation.nova.feature_staking_impl.domain.validations.rebond

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

class RebondValidationPayload(
    val controllerAsset: Asset,
    val chain: Chain,
    val fee: Fee,
    val rebondAmount: BigDecimal
)
