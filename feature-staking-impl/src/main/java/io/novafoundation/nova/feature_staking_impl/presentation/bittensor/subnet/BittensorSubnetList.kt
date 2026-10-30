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

enum class BittensorSubnetEmptyKind {
    /** At least one row is shown, or the list is not settled yet. */
    NONE,

    /** The catalogue itself is empty. */
    NO_SUBNETS,

    /** A non-blank search matches nothing in the unfiltered catalogue: the root row and the controls hide. */
    NO_SEARCH_MATCHES,

    /** Subnets match, but the filters hide all of them: the controls stay so the filters can be changed. */
    FILTERED_OUT
}

/**
 * [queryMatches] counts catalogue subnets matching the search before filters, [shown] the rows left after filters.
 */
internal fun bittensorSubnetEmptyKind(
    loading: Boolean,
    failed: Boolean,
    query: String,
    catalogueSize: Int,
    queryMatches: Int,
    shown: Int
): BittensorSubnetEmptyKind {
    return when {
        loading || failed || shown > 0 -> BittensorSubnetEmptyKind.NONE
        query.isNotBlank() && queryMatches == 0 -> BittensorSubnetEmptyKind.NO_SEARCH_MATCHES
        catalogueSize == 0 -> BittensorSubnetEmptyKind.NO_SUBNETS
        else -> BittensorSubnetEmptyKind.FILTERED_OUT
    }
}

/** Signed weekly change with one decimal, as the iOS picker shows it: "+48.5%", "-3.2%". */
internal fun bittensorPickerChangePercent(changePercent: BigDecimal): String {
    return changePercent.setScale(1, RoundingMode.HALF_EVEN).formatAsChange()
}
