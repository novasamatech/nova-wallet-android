package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.common.StartParachainStakingMode
import kotlinx.android.parcel.Parcelize

@Parcelize
class StartParachainStakingPayload(
    val flowMode: StartParachainStakingMode
): Parcelable
