package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorReportedOperation
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

internal data class BittensorValidatorYield(val hotkey: AccountIdKey, val annualPercentageYield: BigDecimal)

private const val RELIABLE_VERDICT = "RELIABLE"

internal fun mapBittensorReportedOperation(remote: BittensorOperationsRemote.Item): BittensorReportedOperation {
    val netuid = BittensorNetuid.from(requireNotNull(remote.netuid))
    return BittensorReportedOperation(
        netuid = netuid.value,
        hotkey = requireNotNull(remote.hotkey),
        amountIn = requireNotNull(remote.reportedAmountIn),
        amountOut = requireNotNull(remote.reportedAmountOut),
        price = requireNotNull(remote.reportedPrice),
        eventId = requireNotNull(remote.sourceEventId),
        timestamp = requireNotNull(remote.sourceTimestamp),
        reference = requireNotNull(remote.sourceExtrinsicReference),
        type = remote.sourceOperationType
    )
}

/**
 * Maps one provider row to a validator yield.
 *
 * Returns null, and the row is skipped, when the hotkey is absent or not a valid address on [chain],
 * when the backend marks the rate as anything other than `RELIABLE` (ADR-0045: a dust nominated stake
 * compounded into absurd percentages), or when [BittensorAlphaYieldsRemote.Item.reportedRate] is absent,
 * malformed or negative. An absent verdict (older backend) is accepted.
 *
 * @throws IllegalArgumentException when the row belongs to another subnet or metric kind.
 */
internal fun mapBittensorValidatorYield(
    remote: BittensorAlphaYieldsRemote.Item,
    chain: Chain,
    subnetId: BittensorNetuid.Subnet
): BittensorValidatorYield? {
    require(remote.netuid == subnetId.value && remote.metricKind == "ALPHA_VALIDATOR_APY")

    val address = remote.hotkey ?: return null
    val hotkey = runCatching { AccountIdKey(chain.accountIdOf(address)) }
        .getOrNull() ?: return null

    val reliability = remote.rateReliability
    if (reliability != null && reliability != RELIABLE_VERDICT) return null

    val percentage = remote.reportedRate?.toBigDecimalOrNull()
        ?.takeIf { it.isNonNegative } ?: return null

    return BittensorValidatorYield(hotkey, percentage)
}
