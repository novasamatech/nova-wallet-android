package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import java.math.BigDecimal

/** A directory card is informational; only a recommendation can supply an automatic preset. */
data class BittensorValidatorPreview(val validator: BittensorValidator, val canPreselect: Boolean)

internal fun selectBittensorValidatorPreview(
    candidates: List<BittensorValidatorPreview>,
    yields: Map<AccountIdKey, BigDecimal>
): BittensorValidatorPreview? {
    val suggested = candidates.filter { it.canPreselect }
    val available = suggested.ifEmpty { candidates }
    return available.firstOrNull { it.validator.hotkey in yields } ?: available.firstOrNull()
}
