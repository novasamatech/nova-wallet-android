package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.startConfirm.ConfirmStartSingleTargetStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.model.MythosCollatorParcel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.android.parcel.Parcelize

@Parcelize
class ConfirmStartMythosStakingPayload(
    val collator: MythosCollatorParcel,
    override val amount: Balance,
    override val fee: FeeParcelModel,
) : Parcelable, ConfirmStartSingleTargetStakingPayload
