package io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.model

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

open class TargetWithStakedAmount<T : Identifiable>(
    val stake: Balance,
    val target: T
) : Identifiable by target
