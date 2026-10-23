package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.utils.formatting.formatAsChange
import io.novafoundation.nova.common.utils.images.Icon
import java.math.BigDecimal
import java.math.RoundingMode

sealed interface BittensorSubnetListItem {
    data class Root(val icon: Icon) : BittensorSubnetListItem
    data class Subnet(val row: BittensorSubnetRowUi) : BittensorSubnetListItem
}

data class BittensorSubnetListState(
    val items: List<BittensorSubnetListItem>,
    val loading: Boolean
)

/** A search with no matching subnet hides the root entry too, so the no-results state stands alone (as on iOS). */
internal fun bittensorSubnetList(
    rootIcon: Icon,
    subnets: List<BittensorSubnetRowUi>,
    searchHasNoMatches: Boolean = false
): List<BittensorSubnetListItem> {
    val root = if (searchHasNoMatches) emptyList() else listOf(BittensorSubnetListItem.Root(rootIcon))
    return root + subnets.map { BittensorSubnetListItem.Subnet(it) }
}

/** A loaded, non-failed list where a non-blank search matched no subnet. */
internal fun bittensorSearchHasNoMatches(loading: Boolean, failed: Boolean, query: String, matches: Int): Boolean {
    return !loading && !failed && query.isNotBlank() && matches == 0
}

/** Signed weekly change with one decimal, as the iOS picker shows it: "+48.5%", "-3.2%". */
internal fun bittensorPickerChangePercent(changePercent: BigDecimal): String {
    return changePercent.setScale(1, RoundingMode.HALF_EVEN).formatAsChange()
}
