package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState.OptionAdditionalData
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.rewards.NominationPoolRewardCalculator
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType
import kotlinx.coroutines.flow.launchIn
import org.junit.Test

class NominationPoolsRewardCalculatorIntegrationTest : BaseIntegrationTest() {

    private val stakingFeatureComponent = FeatureUtils.getFeature<StakingFeatureComponent>(context, StakingFeatureApi::class.java)

    private val nominationPoolRewardCalculatorFactory = stakingFeatureComponent.nominationPoolRewardCalculatorFactory
    private val stakingUpdateSystem = stakingFeatureComponent.stakingUpdateSystem
    private val stakingSharedState = stakingFeatureComponent.stakingSharedState

    @Test
    fun testRewardCalculator() = runTest {
        val polkadot = chainRegistry.polkadot()
        val stakingOption = StakingOption(
            assetWithChain = ChainWithAsset(polkadot, polkadot.utilityAsset),
            additional = OptionAdditionalData(StakingType.NOMINATION_POOLS)
        )

        stakingSharedState.setSelectedOption(stakingOption)

        stakingUpdateSystem.start()
            .launchIn(this)

        val rewardCalculator = nominationPoolRewardCalculatorFactory.create(stakingOption, sharedComputationScope = this)

        Log.d("NominationPoolsRewardCalculatorIntegrationTest", "Max APY: ${rewardCalculator.maxAPY}")
        Log.d("NominationPoolsRewardCalculatorIntegrationTest", "APY for Nova Pool: ${rewardCalculator.apyFor(54)}")
    }

    private fun NominationPoolRewardCalculator.apyFor(poolId: Int): Double? {
        return apyFor(PoolId(poolId.toBigInteger()))
    }
}
