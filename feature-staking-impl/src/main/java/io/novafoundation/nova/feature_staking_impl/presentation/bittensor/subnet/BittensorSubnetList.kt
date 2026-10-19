package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.utils.images.Icon

sealed interface BittensorSubnetListItem {
    data class Root(val icon: Icon) : BittensorSubnetListItem
    data class Subnet(val row: BittensorSubnetRowUi) : BittensorSubnetListItem
}

internal fun bittensorSubnetList(rootIcon: Icon, subnets: List<BittensorSubnetRowUi>): List<BittensorSubnetListItem> =
    listOf(BittensorSubnetListItem.Root(rootIcon)) + subnets.map { BittensorSubnetListItem.Subnet(it) }
