package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.text.Collator
import java.util.Locale
import java.math.BigDecimal
import io.novafoundation.nova.common.address.AccountIdKey

enum class BittensorValidatorSorting {
    APY,
    TOTAL_STAKE,
    NAME
}

internal fun sortBittensorValidators(
    validators: List<BittensorSubnetValidator>,
    sorting: BittensorValidatorSorting,
    selectedAddress: String?,
    locale: Locale,
    yields: Map<AccountIdKey, BigDecimal> = emptyMap(),
    hideUnusualApy: Boolean = false,
    preselectedAddress: String? = selectedAddress,
    includeUnavailable: Boolean = false
): List<BittensorSubnetValidator> {
    val nameComparator = Collator.getInstance(locale)
    val comparator = compareBy<BittensorSubnetValidator> { it.inactive }
        .thenByDescending { it.address == preselectedAddress }
        .thenByDescending { if (sorting == BittensorValidatorSorting.APY) yields[it.validator.hotkey] else null }
        .thenByDescending { if (sorting != BittensorValidatorSorting.NAME) it.validator.reportedStake else null }
        .thenComparator { first, second -> nameComparator.compare(first.validator.name, second.validator.name) }
        .thenBy { it.address }

    return validators
        .filter { it.canSelect || includeUnavailable }
        .filter { !hideUnusualApy || it.recommended || it.address == selectedAddress || it.address == preselectedAddress }
        .sortedWith(comparator)
}
