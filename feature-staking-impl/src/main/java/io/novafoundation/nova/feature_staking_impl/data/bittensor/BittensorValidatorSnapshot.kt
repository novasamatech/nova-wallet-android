package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

internal data class BittensorValidatorSnapshot(
    val uids: Map<AccountIdKey, Int?>,
    val takes: Map<AccountIdKey, BigInteger?>,
    val permits: List<Boolean>,
    val lastUpdates: List<BigInteger>,
    val activeThreshold: BigInteger
)

internal fun mapBittensorValidators(
    chain: Chain,
    netuid: BittensorNetuid,
    validators: List<BittensorValidator>,
    gates: BittensorValidatorGates,
    snapshot: BittensorValidatorSnapshot
): List<BittensorSubnetValidator> {
    val isRoot = netuid.isRoot()
    return validators.mapNotNull { validator ->
        val uid = snapshot.uids[validator.hotkey] ?: return@mapNotNull null
        val take = snapshot.takes[validator.hotkey] ?: return@mapNotNull null
        if (!gates.allowsTake(take)) return@mapNotNull null
        if (!isRoot && gates.requirePermit && snapshot.permits.getOrNull(uid) != true) return@mapNotNull null
        val active = isRoot || !gates.requireActive || snapshot.lastUpdates.getOrNull(uid)?.let { it >= snapshot.activeThreshold } == true
        BittensorSubnetValidator(
            validator = validator,
            address = chain.addressOf(validator.hotkey),
            take = take.toBigDecimal().divide(BigDecimal(65535), 12, RoundingMode.HALF_EVEN),
            selectable = active
        )
    }
}
