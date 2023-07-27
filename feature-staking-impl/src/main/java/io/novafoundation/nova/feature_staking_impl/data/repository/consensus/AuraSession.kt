package io.novafoundation.nova.feature_staking_impl.data.repository.consensus

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.committeeManagementOrNull
import io.novafoundation.nova.common.utils.electionsOrNull
import io.novafoundation.nova.common.utils.numberConstantOrNull
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import java.math.BigInteger

private const val SESSION_PERIOD_DEFAULT = 50

class AuraSession(
    private val chainRegistry: ChainRegistry,
    private val remoteStorage: StorageDataSource,
) : ElectionsSession {

    override suspend fun sessionLength(chainId: ChainId): BigInteger {
        val runtime = runtimeFor(chainId)

        return runtime.metadata.electionsOrNull()?.numberConstantOrNull("SessionPeriod", runtime)
            ?: runtime.metadata.committeeManagementOrNull()?.numberConstantOrNull("SessionPeriod", runtime)
            ?: SESSION_PERIOD_DEFAULT.toBigInteger()
    }

    override suspend fun currentSlot(chainId: ChainId) = remoteStorage.query(chainId) {
        val bestBlock = runtime.metadata.system().storage("Number").query(binding = ::bindNumber)

        bestBlock
    }

    override suspend fun genesisSlot(chainId: ChainId): BigInteger = BigInteger.ZERO

    private suspend fun runtimeFor(chainId: ChainId): RuntimeSnapshot {
        return chainRegistry.getRuntime(chainId)
    }
}
