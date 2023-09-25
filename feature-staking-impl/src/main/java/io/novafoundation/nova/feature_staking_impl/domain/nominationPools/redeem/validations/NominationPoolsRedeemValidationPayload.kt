package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.validations

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

class NominationPoolsRedeemValidationPayload(
    val fee: BigDecimal,
    val asset: Asset,
)
