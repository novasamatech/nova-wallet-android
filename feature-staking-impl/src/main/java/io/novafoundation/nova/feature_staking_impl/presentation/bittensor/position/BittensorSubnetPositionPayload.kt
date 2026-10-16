package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId

@Parcelize
data class BittensorSubnetPositionPayload(val subnetId: Int) : Parcelable {

    fun subnetId() = BittensorSubnetId(subnetId)
}
