package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.AvailableStakingOptionsPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
class SetupAmountMultiStakingPayload(val availableStakingOptions: AvailableStakingOptionsPayload) : Parcelable

