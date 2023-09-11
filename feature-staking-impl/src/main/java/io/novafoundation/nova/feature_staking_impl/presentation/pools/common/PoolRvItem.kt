package io.novafoundation.nova.feature_staking_impl.presentation.pools.common

import io.novafoundation.nova.feature_staking_api.presentation.nominationPools.display.PoolDisplayModel
import java.math.BigInteger

class PoolRvItem(
    val id: BigInteger,
    val address: String,
    val model: PoolDisplayModel,
    val subtitle: CharSequence,
    val members: CharSequence,
    val isChecked: Boolean
)
