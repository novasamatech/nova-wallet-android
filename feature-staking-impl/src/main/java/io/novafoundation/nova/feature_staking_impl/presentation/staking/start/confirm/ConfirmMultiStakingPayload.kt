package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.AvailableStakingOptionsPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.parcelize.Parcelize

@Parcelize
class ConfirmMultiStakingPayload(val fee: FeeParcelModel, val availableStakingOptions: AvailableStakingOptionsPayload) : Parcelable
