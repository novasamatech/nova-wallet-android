package io.novafoundation.nova.feature_staking_impl.presentation.pools.common

import io.novafoundation.nova.common.utils.images.Icon
import java.math.BigInteger

class PoolRvItem(
    val id: BigInteger,
    val title: CharSequence,
    val subtitle: CharSequence,
    val members: CharSequence,
    val isChecked: Boolean,
    val icon: Icon
)
