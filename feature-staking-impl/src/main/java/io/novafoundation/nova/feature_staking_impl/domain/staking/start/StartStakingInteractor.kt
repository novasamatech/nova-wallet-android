package io.novafoundation.nova.feature_staking_impl.domain.staking.start

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.PayoutType
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow

class StartStakingData(
    val availableBalance: BigInteger,
    val maxEarningRate: BigDecimal,
    val minStake: BigInteger,
    val payoutType: PayoutType,
    val participationInGovernance: Boolean
)

interface StartStakingInteractor {

    fun observeData(chain: Chain, asset: Asset): Flow<StartStakingData>
}
