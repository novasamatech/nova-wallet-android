package io.novafoundation.nova.feature_staking_impl.presentation.mappers

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatAsSpannable
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.formatPercents
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.apy
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.PoolRvItem
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

suspend fun mapNominationPoolToPoolRvItem(
    chain: Chain,
    pool: NominationPool,
    resourceManager: ResourceManager,
    poolDisplayFormatter: PoolDisplayFormatter,
    isChecked: Boolean
): PoolRvItem {
    val model = poolDisplayFormatter.format(pool, chain)
    return PoolRvItem(
        id = pool.id.value,
        model = model,
        subtitle = getSubtitle(pool, resourceManager),
        members = pool.membersCount.format(),
        isChecked = isChecked
    )
}

private fun getSubtitle(
    pool: NominationPool,
    resourceManager: ResourceManager,
): CharSequence {
    val apyColor = resourceManager.getColor(R.color.text_positive)
    val apy = pool.apy.orZero()
    val apyString = apy.formatPercents().toSpannable(colorSpan(apyColor))
    return resourceManager.getString(R.string.common_per_year_format)
        .formatAsSpannable(apyString)
}
