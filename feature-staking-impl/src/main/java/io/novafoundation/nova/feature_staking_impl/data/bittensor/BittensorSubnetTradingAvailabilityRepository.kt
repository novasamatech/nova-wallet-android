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

    /** One multi-key storage read for the whole list, so a slow node costs one round trip, not one per subnet. */
    suspend fun enabledSubnets(subnetIds: List<BittensorNetuid.Subnet>): Set<BittensorNetuid.Subnet> {
        if (subnetIds.isEmpty()) return emptySet()
        val enabled = storage.query(ChainGeneses.BITTENSOR) {
            runtime.metadata.module("SubtensorModule")
                .storage("SubtokenEnabled")
                .entries(
                    keysArguments = subnetIds.map { listOf(it.value.toBigInteger()) },
                    keyExtractor = { key -> BittensorNetuid.Subnet(bindBittensorU16(key.component1())) },
                    binding = { value, _ -> value?.let(::bindBoolean) ?: false }
                )
        }
        return subnetIds.filterTo(mutableSetOf()) { enabled[it] == true }
    }
}
