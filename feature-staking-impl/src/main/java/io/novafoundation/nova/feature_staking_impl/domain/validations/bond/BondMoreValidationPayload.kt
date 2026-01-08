package io.novafoundation.nova.feature_staking_impl.domain.validations.bond

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

class BondMoreValidationPayload(
    val stashAddress: String,
    val fee: Fee,
    val chain: Chain,
    val amount: BigDecimal,
    val stashAsset: Asset,
    val stakeable: BigDecimal
)
