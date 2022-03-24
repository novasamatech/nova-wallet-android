package io.novafoundation.nova.feature_staking_impl.presentation.payouts.detail

import androidx.annotation.ColorRes
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class PayoutDetailsModel(
    val validatorAddressModel: AddressModel,
    val timeLeft: Long,
    val timeLeftCalculatedAt: Long,
    @ColorRes val timerColor: Int,
    val eraDisplay: String,
    val reward: AmountModel,
)
