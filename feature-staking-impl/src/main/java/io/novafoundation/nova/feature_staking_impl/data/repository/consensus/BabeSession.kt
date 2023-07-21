package io.novafoundation.nova.feature_staking_impl.data.repository.consensus

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.babe
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindCurrentSlot
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.queryNonNull
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import java.math.BigInteger

class BabeSession(
    private val remoteStorage: StorageDataSource,
    private val chainRegistry: ChainRegistry,
) : ElectionsSession {

    override suspend fun sessionLength(chainId: ChainId): BigInteger {
        val runtime = runtimeFor(chainId)

        return runtime.metadata.babe().numberConstant("EpochDuration", runtime)
    }

    override suspend fun currentEpochIndex(chainId: ChainId): BigInteger {
        return remoteStorage.query(chainId) {
            runtime.metadata.babe().storage("EpochIndex").query(binding = ::bindNumber)
        }
    }

    override suspend fun currentSlot(chainId: ChainId) = remoteStorage.queryNonNull(
        keyBuilder = { it.metadata.babe().storage("CurrentSlot").storageKey() },
        binding = ::bindCurrentSlot,
        chainId = chainId
    )

    override suspend fun genesisSlot(chainId: ChainId) = remoteStorage.queryNonNull(
        keyBuilder = { it.metadata.babe().storage("GenesisSlot").storageKey() },
        binding = ::bindCurrentSlot,
        chainId = chainId
    )

    private suspend fun runtimeFor(chainId: ChainId): RuntimeSnapshot {
        return chainRegistry.getRuntime(chainId)
    }
}
