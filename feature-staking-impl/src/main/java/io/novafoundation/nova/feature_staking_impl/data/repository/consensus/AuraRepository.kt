package io.novafoundation.nova.feature_staking_impl.data.repository.consensus

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.aura
import io.novafoundation.nova.common.utils.hasModule
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.timestamp
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import java.math.BigInteger

class AuraRepository(
    private val chainRegistry: ChainRegistry,
    private val remoteStorage: StorageDataSource,
) : ConsensusRepository {

    override suspend fun consensusAvailable(chainId: ChainId): Boolean {
        val metadata = runtimeFor(chainId).metadata

        return metadata.hasModule(Modules.AURA) && metadata.hasModule(Modules.TIMESTAMP)
    }

    override suspend fun sessionLength(chainId: ChainId): BigInteger {
        val runtime = runtimeFor(chainId)

        return runtime.metadata.aura().numberConstant("SessionPeriod", runtime)
    }

    override suspend fun currentSlot(chainId: ChainId) = remoteStorage.query(chainId) {
        val now = runtime.metadata.timestamp().storage("Now").query(binding = ::bindNumber)

        val blockDuration = runtime.metadata.timestamp().numberConstant("MinimumPeriod", runtime)
        val slotDuration = blockDuration * 2.toBigInteger()

        now / slotDuration
    }

    override suspend fun genesisSlot(chainId: ChainId): BigInteger = BigInteger.ZERO

    private suspend fun runtimeFor(chainId: ChainId): RuntimeSnapshot {
        return chainRegistry.getRuntime(chainId)
    }
}
