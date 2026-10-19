package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetParametersSource
import javax.inject.Inject

class BittensorSubnetParametersInteractor @Inject constructor(
    private val source: BittensorSubnetParametersSource
) {

    suspend fun emission(reference: BittensorSubnetReference): BittensorSubnetEmission {
        val snapshot = source.emissionSnapshot(reference.subnetId)
        return BittensorSubnetEmission(reference, bittensorSubnetEmissionShare(reference, snapshot))
    }
}
