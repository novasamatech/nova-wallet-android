package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.HelperBinding
import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.getList
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.data.network.runtime.binding.requireType
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.feature_staking_api.domain.model.StakingLedger
import io.novafoundation.nova.feature_staking_api.domain.model.UnlockChunk
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage

@UseCaseBinding
fun bindStakingLedger(scale: String, runtime: RuntimeSnapshot): StakingLedger {
    val type = runtime.metadata.staking().storage("Ledger").returnType()
    val dynamicInstance = type.fromHexOrNull(runtime, scale) ?: incompatible()

    return bindStakingLedger(dynamicInstance)
}

@UseCaseBinding
fun bindStakingLedger(dynamicInstance: Any): StakingLedger {
    requireType<Struct.Instance>(dynamicInstance)

    return StakingLedger(
        stashId = dynamicInstance.getTyped("stash"),
        total = dynamicInstance.getTyped("total"),
        active = dynamicInstance.getTyped("active"),
        unlocking = dynamicInstance.getList("unlocking").map(::bindUnlockChunk),
        claimedRewards = dynamicInstance.getList("claimedRewards").map(::bindEraIndex)
    )
}

@UseCaseBinding
fun bindStakingLedgerOrNull(dynamicInstance: Any?): StakingLedger? {
    return dynamicInstance?.let(::bindStakingLedger)
}

@HelperBinding
fun bindUnlockChunk(dynamicInstance: Any?): UnlockChunk {
    requireType<Struct.Instance>(dynamicInstance)

    return UnlockChunk(
        amount = dynamicInstance.getTyped("value"),
        era = dynamicInstance.getTyped("era")
    )
}
