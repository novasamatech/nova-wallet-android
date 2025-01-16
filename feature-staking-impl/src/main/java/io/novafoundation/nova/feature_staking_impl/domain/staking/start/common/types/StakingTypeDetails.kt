package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.feature_staking_impl.domain.model.PayoutType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class StakingTypeDetails(
    val maxEarningRate: Fraction,
    val minStake: BigInteger,
    val payoutType: PayoutType,
    val participationInGovernance: Boolean,
    val advancedOptionsAvailable: Boolean,
    val stakingType: Chain.Asset.StakingType
)
