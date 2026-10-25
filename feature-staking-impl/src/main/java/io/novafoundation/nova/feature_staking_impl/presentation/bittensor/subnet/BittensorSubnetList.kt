package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.utils.images.Icon

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
