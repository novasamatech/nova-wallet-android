package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.images.Icon

enum class BittensorSubnetGroup { ROOT, SUBNETS }

sealed interface BittensorSubnetListItem {
    data class Root(val icon: Icon, val tokenSymbol: TokenSymbol) : BittensorSubnetListItem
    data class Subnet(val row: BittensorSubnetRowUi) : BittensorSubnetListItem
}

internal fun bittensorSubnetList(rootIcon: Icon, subnets: List<BittensorSubnetRowUi>, tokenSymbol: TokenSymbol): List<Any> = linkedMapOf(
    BittensorSubnetGroup.ROOT to listOf<BittensorSubnetListItem>(BittensorSubnetListItem.Root(rootIcon, tokenSymbol)),
    BittensorSubnetGroup.SUBNETS to subnets.map { BittensorSubnetListItem.Subnet(it) }
).toListWithHeaders()
