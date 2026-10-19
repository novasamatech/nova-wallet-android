package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.storage

class BittensorSubnetTradingAvailabilityRepository(private val storage: StorageDataSource) {

    suspend fun isEnabled(subnetId: BittensorNetuid.Subnet): Boolean = storage.query(ChainGeneses.BITTENSOR) {
        runtime.metadata.module("SubtensorModule")
            .storage("SubtokenEnabled")
            .query(subnetId.value.toBigInteger()) { it?.let(::bindBoolean) ?: false }
    }
}
