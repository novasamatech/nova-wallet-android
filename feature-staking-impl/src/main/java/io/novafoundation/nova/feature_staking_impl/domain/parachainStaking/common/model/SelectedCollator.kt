package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

open class SelectedCollator(
    val collator: Collator,
    val delegation: Balance,
) : Identifiable by collator
