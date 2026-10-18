package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorReportedOperation
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

internal data class BittensorValidatorYield(val hotkey: AccountIdKey, val annualPercentageYield: BigDecimal)

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

internal fun mapBittensorValidatorYield(
    remote: BittensorAlphaYieldsRemote.Item,
    chain: Chain,
    subnetId: BittensorNetuid.Subnet
): BittensorValidatorYield? {
    require(remote.netuid == subnetId.value && remote.metricKind == "ALPHA_VALIDATOR_APY")

    val address = remote.hotkey ?: return null
    val hotkey = runCatching { AccountIdKey(chain.accountIdOf(address)) }
        .getOrNull() ?: return null

    val percentage = remote.reportedRate?.toBigDecimalOrNull()
        ?.takeIf { it.isNonNegative } ?: return null

    return BittensorValidatorYield(hotkey, percentage)
}
