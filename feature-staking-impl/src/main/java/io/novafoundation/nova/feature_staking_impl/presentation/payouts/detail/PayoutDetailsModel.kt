package io.novafoundation.nova.feature_staking_impl.presentation.payouts.detail

import io.novafoundation.nova.common.address.AddressModel

class PayoutDetailsModel(
    val validatorAddressModel: AddressModel,
    val createdAt: Long,
    val eraDisplay: String,
    val reward: String,
    val rewardFiat: String?
)
