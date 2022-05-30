package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class SelectCollatorModel(
    val addressModel: AddressModel,
    val staked: AmountModel?,
    val collator: Collator,
)
