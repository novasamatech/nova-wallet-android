package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.AvailableStakingOptionsPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
class SetupStakingTypePayload(val availableStakingOptions: AvailableStakingOptionsPayload) : Parcelable
