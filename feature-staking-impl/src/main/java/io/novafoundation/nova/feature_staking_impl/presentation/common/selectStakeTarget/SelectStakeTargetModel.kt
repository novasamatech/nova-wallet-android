package io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget

import androidx.annotation.StringRes
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class SelectStakeTargetModel<out T : Identifiable>(
    val addressModel: AddressModel,
    val amount: AmountModel?,
    val active: Boolean,
    val payload: T,
    @StringRes val amountLabelRes: Int = R.string.staking_main_stake_balance_staked
) : Identifiable by payload
