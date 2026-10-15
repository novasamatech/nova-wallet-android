package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflight
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import java.math.BigInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

interface BittensorStakePreflightRepository {

    suspend fun load(chainId: String, netuid: BittensorNetuid, coldKey: AccountId, hotkey: AccountId): BittensorStakePreflight
}

class RealBittensorStakePreflightRepository(
    private val storage: StorageDataSource,
    private val rpc: RpcCalls
) : BittensorStakePreflightRepository {

    override suspend fun load(
        chainId: String,
        netuid: BittensorNetuid,
        coldKey: AccountId,
        hotkey: AccountId
    ): BittensorStakePreflight {
        val blockHash = rpc.getBlockHash(chainId)
        return storage.query(chainId, at = blockHash) {
            loadPreflight(netuid, coldKey, hotkey)
        }
    }
}

internal suspend fun StorageQueryContext.loadPreflight(
    netuid: BittensorNetuid,
    coldKey: AccountId,
    hotkey: AccountId
): BittensorStakePreflight = coroutineScope {
    val pallet = runtime.metadata.module("SubtensorModule")
    val storageNetuid = netuid.value.toBigInteger()
    val hotkeyRegistered = async { pallet.storage("Owner").queryRaw(hotkey) != null }
    val subnetExists = async {
        pallet.storage("NetworksAdded")
            .query(storageNetuid) { it?.let(::bindBoolean) ?: false }
    }
    val stakingEnabled = async {
        pallet.storage("SubtokenEnabled")
            .query(storageNetuid) { it?.let(::bindBoolean) ?: false }
    }
    val coldKeySwapPending = async { pallet.storage("ColdkeySwapAnnouncements").queryRaw(coldKey) != null }
    val safeModeActive = async {
        runtime.metadata.module("SafeMode")
            .storage("EnteredUntil")
            .queryRaw() != null
    }
    val currentBlock = async {
        runtime.metadata.module("System")
            .storage("Number")
            .query(binding = ::bindNumber)
    }
    val rootUnlockInterval = async {
        pallet.storage("RootStakeUnlockInterval")
            .query { it?.let(::bindNumber) ?: BigInteger.ZERO }
    }
    val lastStakeBlock = async {
        pallet.storage("LastColdkeyHotkeyStakeBlock")
            .query(coldKey, hotkey) { it?.let(::bindNumber) }
    }

    BittensorStakePreflight(
        hotkeyRegistered = hotkeyRegistered.await(),
        subnetExists = subnetExists.await(),
        stakingEnabled = stakingEnabled.await(),
        coldKeySwapPending = coldKeySwapPending.await(),
        safeModeActive = safeModeActive.await(),
        minimumStake = pallet.numberConstant("InitialMinStake", runtime),
        currentBlock = currentBlock.await(),
        rootUnlockInterval = rootUnlockInterval.await(),
        lastStakeBlock = lastStakeBlock.await()
    )
}
