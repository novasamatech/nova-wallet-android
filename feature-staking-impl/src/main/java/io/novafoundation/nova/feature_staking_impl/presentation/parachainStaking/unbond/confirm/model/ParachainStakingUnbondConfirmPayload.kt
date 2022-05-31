package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.model

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.CollatorParcelModel
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class ParachainStakingUnbondConfirmPayload(
    val collator: CollatorParcelModel,
    val amount: BigDecimal,
    val fee: BigDecimal
) : Parcelable
