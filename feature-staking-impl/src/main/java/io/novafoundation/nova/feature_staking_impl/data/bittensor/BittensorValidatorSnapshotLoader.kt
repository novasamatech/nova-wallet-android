package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import java.math.BigInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal suspend fun StorageQueryContext.loadBittensorValidatorSnapshot(
    netuid: BittensorNetuid,
    validators: List<BittensorValidator>,
    gates: BittensorValidatorGates
): BittensorValidatorSnapshot = coroutineScope {
    val pallet = runtime.metadata.module("SubtensorModule")
    val storageNetuid = netuid.value.toBigInteger()
    val permits = async {
        if (!netuid.isRoot() && gates.requirePermit) {
            pallet.storage("ValidatorPermit")
                .query(storageNetuid) { bindList(it, ::bindBoolean) }
        } else {
            emptyList()
        }
    }
    val updates = async {
        if (!netuid.isRoot() && gates.requireActive) {
            pallet.storage("LastUpdate")
                .query(storageNetuid) { bindList(it, ::bindNumber) }
        } else {
            emptyList()
        }
    }
    val activeThreshold = async {
        if (!netuid.isRoot() && gates.requireActive) loadActiveThreshold(storageNetuid) else BigInteger.ZERO
    }
    val uids = async {
        pallet.storage("Uids")
            .entries(
                keysArguments = validators.map { listOf(storageNetuid, it.hotkey.value) },
                keyExtractor = { key -> AccountIdKey(key.component2()) },
                binding = { value, _ -> value?.let(::bindBittensorU16) }
            )
    }
    val commissionsInU16 = async {
        pallet.storage("Delegates")
            .entries(
                keysArguments = validators.map { listOf(it.hotkey.value) },
                keyExtractor = { key -> AccountIdKey(key.component1()) },
                binding = { value, _ -> value?.let(::bindNumber) }
            )
    }

    BittensorValidatorSnapshot(
        uids = uids.await(),
        commissionsInU16 = commissionsInU16.await(),
        permits = permits.await(),
        lastUpdates = updates.await(),
        activeThreshold = activeThreshold.await()
    )
}

private suspend fun StorageQueryContext.loadActiveThreshold(netuid: BigInteger): BigInteger = coroutineScope {
    val pallet = runtime.metadata.module("SubtensorModule")
    val block = async {
        runtime.metadata.module("System")
            .storage("Number")
            .query(binding = ::bindNumber)
    }
    val tempo = async {
        pallet.storage("Tempo")
            .query(netuid, binding = ::bindNumber)
    }
    val cutoffFactor = async {
        pallet.storage("ActivityCutoffFactorMilli")
            .query(netuid, binding = ::bindNumber)
    }

    val activityCutoff = (tempo.await() * cutoffFactor.await() / BigInteger.valueOf(1000))
        .max(BigInteger.ONE)
    block.await() - activityCutoff
}
