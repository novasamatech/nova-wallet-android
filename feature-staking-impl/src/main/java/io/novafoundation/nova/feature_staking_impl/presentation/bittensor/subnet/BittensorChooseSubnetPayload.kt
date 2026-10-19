package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BittensorChooseSubnetPayload(val replaceAmount: Boolean = false) : Parcelable
