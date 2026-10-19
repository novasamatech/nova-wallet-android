package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetEmissionSnapshot
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

class BittensorSubnetParametersSource @Inject constructor(
    private val runtimeCalls: MultiChainRuntimeCallsApi,
    @Named(REMOTE_STORAGE_SOURCE) private val storage: StorageDataSource,
    private val rpcCalls: RpcCalls
) {

    suspend fun emissionSnapshot(
        subnetId: BittensorNetuid.Subnet
    ): BittensorSubnetEmissionSnapshot = withTimeoutOrNull(SNAPSHOT_TIMEOUT_MILLIS) {
        val chainId = ChainGeneses.BITTENSOR
        val hash = rpcCalls.getBlockHash(chainId)
        coroutineScope {
            val rows = async {
                runtimeCalls.forChain(chainId).callAt(
                    blockHash = hash,
                    section = "SubnetInfoRuntimeApi",
                    method = "get_all_dynamic_info",
                    arguments = emptyMap(),
                    returnBinding = ::bindBittensorEmissionRows
                )
            }
            val enabled = async {
                storage.query(chainId, at = hash, applyStorageDefault = false) {
                    runtime.metadata.module("SubtensorModule").storage("SubnetEmissionEnabled")
                        .query(subnetId.value.toBigInteger(), binding = { it?.let(::bindBoolean) })
                }
            }
            BittensorSubnetEmissionSnapshot(rows.await(), enabled.await())
        }
    } ?: error("Subnet parameter snapshot timed out")

    private companion object {

        const val SNAPSHOT_TIMEOUT_MILLIS = 20_000L
    }
}
