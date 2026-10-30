package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import kotlinx.parcelize.Parcelize

@Parcelize
data class BittensorSubnetPositionPayload(val subnetId: Int) : Parcelable {

    fun subnetId() = BittensorNetuid.Subnet(subnetId)
}
