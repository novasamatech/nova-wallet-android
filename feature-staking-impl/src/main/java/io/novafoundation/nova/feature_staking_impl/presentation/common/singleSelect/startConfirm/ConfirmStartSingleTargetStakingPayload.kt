package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.startConfirm

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel

interface ConfirmStartSingleTargetStakingPayload {

    val fee: FeeParcelModel

    val amount: Balance
}
