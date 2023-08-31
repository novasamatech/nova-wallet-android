package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.CollatorParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.common.StartParachainStakingMode
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class ConfirmStartParachainStakingPayload(
    val collator: CollatorParcelModel,
    val amount: BigDecimal,
    val fee: BigDecimal,
    val flowMode: StartParachainStakingMode
) : Parcelable
