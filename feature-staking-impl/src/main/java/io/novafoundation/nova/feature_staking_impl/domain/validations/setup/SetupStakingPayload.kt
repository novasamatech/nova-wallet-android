package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset

class SetupStakingPayload(
    val maxFee: Fee,
    val controllerAsset: Asset,
)
