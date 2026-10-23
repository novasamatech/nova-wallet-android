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

// Subtensor's DefaultDelegateTake: the take of a hotkey without a Delegates entry, as iOS defaultDelegateTake.
private val DEFAULT_DELEGATE_TAKE_IN_U16 = 11_796.toBigInteger()

internal data class BittensorValidatorSnapshot(
    val uids: Map<AccountIdKey, Int?>,
    val commissionsInU16: Map<AccountIdKey, BigInteger?>,
    val permits: List<Boolean>,
    val lastUpdates: List<BigInteger>,
    val activeThreshold: BigInteger,
    val registeredAt: BigInteger? = null
)

/**
 * Every validator is listed, as on iOS: the take limit and the permit and activity gates only decide recommendations
 * (see [passesBittensorRecommendationGates]), and only an inactive validator cannot take a new stake.
 */
internal fun mapBittensorValidators(
    chain: Chain,
    netuid: BittensorNetuid,
    validators: List<BittensorValidator>,
    snapshot: BittensorValidatorSnapshot
): List<BittensorSubnetValidator> {
    return validators.map { validator -> mapBittensorValidator(chain, netuid, validator, snapshot) }
}

private fun mapBittensorValidator(
    chain: Chain,
    netuid: BittensorNetuid,
    validator: BittensorValidator,
    snapshot: BittensorValidatorSnapshot
): BittensorSubnetValidator {
    val inactive = snapshot.isInactive(netuid, validator.hotkey)

    return BittensorSubnetValidator(
        validator = validator,
        address = chain.addressOf(validator.hotkey),
        commissionRate = validatorCommissionRateFromU16(snapshot.commissionInU16(validator.hotkey)),
        selectable = !inactive,
        inactive = inactive,
        // Buy more on an inactive validator asks to accept the warning instead.
        canAddToExistingPosition = true
    )
}

internal fun BittensorValidatorSnapshot.commissionInU16(hotkey: AccountIdKey): BigInteger = commissionsInU16[hotkey] ?: DEFAULT_DELEGATE_TAKE_IN_U16

/**
 * As iOS SubtensorRecommendationVerifier.passesGates: only a validator registered on [netuid] with a readable chain status,
 * a take within the limit and, on a subnet, the permit and activity the [gates] require can be recommended.
 */
internal fun BittensorValidatorSnapshot.passesBittensorRecommendationGates(
    netuid: BittensorNetuid,
    hotkey: AccountIdKey,
    gates: BittensorValidatorGates
): Boolean {
    val uid = uids[hotkey] ?: return false
    if (!gates.allowsCommission(commissionInU16(hotkey))) return false
    if (netuid.isRoot()) return true
    val hasPermit = permits.getOrNull(uid) ?: return false
    val lastUpdate = lastUpdates.getOrNull(uid) ?: return false
    return (!gates.requirePermit || hasPermit) && (!gates.requireActive || lastUpdate >= activeThreshold)
}

internal fun BittensorValidatorSnapshot.isInactive(netuid: BittensorNetuid, hotkey: AccountIdKey): Boolean {
    if (netuid.isRoot()) return false
    val uid = uids[hotkey] ?: return true
    val hasPermit = permits.getOrNull(uid) ?: return false
    val lastUpdate = lastUpdates.getOrNull(uid) ?: return false
    return !hasPermit || lastUpdate < activeThreshold
}
