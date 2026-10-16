package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validatorCommissionRateFromU16
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

internal data class BittensorValidatorSnapshot(
    val uids: Map<AccountIdKey, Int?>,
    val commissionsInU16: Map<AccountIdKey, BigInteger?>,
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
    return validators.mapNotNull { validator -> mapBittensorValidator(chain, netuid, validator, gates, snapshot) }
}

private fun mapBittensorValidator(
    chain: Chain,
    netuid: BittensorNetuid,
    validator: BittensorValidator,
    gates: BittensorValidatorGates,
    snapshot: BittensorValidatorSnapshot
): BittensorSubnetValidator? {
    val uid = snapshot.uids[validator.hotkey] ?: return null
    val commissionInU16 = snapshot.commissionsInU16[validator.hotkey] ?: return null
    if (!gates.allowsCommission(commissionInU16)) return null
    if (!netuid.isRoot() && snapshot.permits.getOrNull(uid) != true) return null

    return BittensorSubnetValidator(
        validator = validator,
        address = chain.addressOf(validator.hotkey),
        commissionRate = validatorCommissionRateFromU16(commissionInU16),
        selectable = isValidatorActive(netuid, uid, snapshot)
    )
}

private fun isValidatorActive(netuid: BittensorNetuid, uid: Int, snapshot: BittensorValidatorSnapshot): Boolean {
    if (netuid.isRoot()) return true
    val lastUpdate = snapshot.lastUpdates.getOrNull(uid) ?: return false
    return lastUpdate >= snapshot.activeThreshold
}
