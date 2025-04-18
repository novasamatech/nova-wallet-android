package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.startConfirm.ConfirmStartSingleTargetStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.CollatorParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.common.StartParachainStakingMode
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.parcelize.Parcelize

@Parcelize
class ConfirmStartParachainStakingPayload(
    val collator: CollatorParcelModel,
    override val amount: Balance,
    override val fee: FeeParcelModel,
    val flowMode: StartParachainStakingMode
) : Parcelable, ConfirmStartSingleTargetStakingPayload
