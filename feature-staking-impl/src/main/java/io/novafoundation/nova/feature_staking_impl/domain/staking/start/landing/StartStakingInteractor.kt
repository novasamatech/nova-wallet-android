package io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.model.PayoutType
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.BigInteger

class StartStakingData(
    val maxEarningRate: BigDecimal,
    val minStake: BigInteger,
    val payoutType: PayoutType,
    val participationInGovernance: Boolean
)

interface StartStakingInteractor {

    fun observeData(): Flow<StartStakingData>

    fun getAvailableBalance(asset: Asset): BigInteger
}
