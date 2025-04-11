package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.AvailableStakingOptionsPayload
import kotlinx.parcelize.Parcelize

@Parcelize
class StartStakingLandingPayload(val availableStakingOptions: AvailableStakingOptionsPayload) : Parcelable
