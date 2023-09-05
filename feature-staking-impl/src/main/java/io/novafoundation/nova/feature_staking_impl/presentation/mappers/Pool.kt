package io.novafoundation.nova.feature_staking_impl.presentation.mappers

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createAddressIcon
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.asPerbill
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatAsSpannable
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.apy
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.PoolRvItem
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

private const val ICON_SIZE_DP = 24

suspend fun mapNominationPoolToPoolRvItem(
    chain: Chain,
    pool: NominationPool,
    iconGenerator: AddressIconGenerator,
    resourceManager: ResourceManager,
    isChecked: Boolean
): PoolRvItem {
    val poolName = pool.metadata?.title
    val address = chain.addressOf(pool.stashAccountId)
    val icon = pool.icon ?: iconGenerator.createAddressIcon(address, ICON_SIZE_DP, AddressIconGenerator.BACKGROUND_TRANSPARENT).asIcon()

    return PoolRvItem(
        id = pool.id.value,
        title = poolName ?: address,
        subtitle = getSubtitle(pool, resourceManager),
        members = pool.membersCount.format(),
        isChecked = isChecked,
        icon = icon
    )
}

private fun getSubtitle(
    pool: NominationPool,
    resourceManager: ResourceManager,
): CharSequence {
    val apyColor = resourceManager.getColor(R.color.text_positive)
    val apy = pool.apy ?: 0.0.asPerbill()
    val apyString = apy.format().toSpannable(colorSpan(apyColor))
    return resourceManager.getString(R.string.pool_item_subtitle_apy)
        .formatAsSpannable(apyString)
}
