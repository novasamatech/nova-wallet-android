package io.novafoundation.nova.feature_staking_impl.data.repository.consensus

import io.novafoundation.nova.common.utils.committeeManagementOrNull
import io.novafoundation.nova.common.utils.electionsOrNull
import io.novafoundation.nova.common.utils.numberConstantOrNull
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.updaters.SharedAssetBlockNumberUpdater
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novafoundation.nova.runtime.storage.typed.number
import io.novafoundation.nova.runtime.storage.typed.system
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.math.BigInteger

private const val SESSION_PERIOD_DEFAULT = 50

class AuraSession(
    private val chainRegistry: ChainRegistry,
    private val localStorage: StorageDataSource,
) : ElectionsSession {

    override suspend fun sessionLength(chainId: ChainId): BigInteger {
        val runtime = runtimeFor(chainId)

        return runtime.metadata.electionsOrNull()?.numberConstantOrNull("SessionPeriod", runtime)
            ?: runtime.metadata.committeeManagementOrNull()?.numberConstantOrNull("SessionPeriod", runtime)
            ?: SESSION_PERIOD_DEFAULT.toBigInteger()
    }

    override fun currentEpochIndexFlow(chainId: ChainId): Flow<BigInteger?> {
        return flowOf(null)
    }

    override fun currentSlotFlow(chainId: ChainId) = localStorage.subscribe(chainId) {
        metadata.system.number.observeNonNull()
    }

    override suspend fun currentSlotStorageKey(chainId: ChainId): String? {
        /**
         * we're already syncing system number as part of [SharedAssetBlockNumberUpdater]
         */
        return null
    }

    override suspend fun genesisSlotStorageKey(chainId: ChainId): String? {
        // genesis slot for aura is zero so nothing to sync
        return null
    }

    override suspend fun currentEpochIndexStorageKey(chainId: ChainId): String? {
        // there is no separate epoch index for aura
        return null
    }

    override suspend fun genesisSlot(chainId: ChainId): BigInteger = BigInteger.ZERO

    private suspend fun runtimeFor(chainId: ChainId): RuntimeSnapshot {
        return chainRegistry.getRuntime(chainId)
    }
}
