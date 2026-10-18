package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.withTimeout

interface BittensorExitRuntimeSource {

    suspend fun preflight(
        chainId: String,
        coldkey: AccountId,
        hotkey: AccountId,
        netuid: BittensorNetuid
    ): BittensorExitPreflight
}

class RealBittensorExitRuntimeSource(
    private val storage: StorageDataSource,
    private val rpcCalls: RpcCalls,
    private val chainRegistry: ChainRegistry
) : BittensorExitRuntimeSource {

    override suspend fun preflight(
        chainId: String,
        coldkey: AccountId,
        hotkey: AccountId,
        netuid: BittensorNetuid
    ): BittensorExitPreflight = withTimeout(
        25_000
    ) {
        val hash = rpcCalls.getBlockHash(chainId)
        val socket = chainRegistry.getSocket(chainId)
        storage.query(
            chainId,
            at = hash,
            applyStorageDefault = true
        ) {
            loadBittensorExitSnapshot(
                coldkey,
                hotkey,
                netuid,
                socket,
                hash
            )
        }
    }
}
