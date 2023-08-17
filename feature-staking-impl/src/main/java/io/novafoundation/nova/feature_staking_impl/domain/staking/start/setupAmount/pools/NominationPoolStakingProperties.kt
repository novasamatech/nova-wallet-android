package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.CoroutineScope

class NominationPoolStakingPropertiesFactory(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    ): SingleStakingPropertiesFactory {

    override fun createProperties(scope: CoroutineScope, stakingOption: StakingOption): SingleStakingProperties {
        return NominationPoolStakingProperties(
            nominationPoolSharedComputation = nominationPoolSharedComputation,
            sharedComputationScope = scope,
            stakingOption = stakingOption
        )
    }
}

private class NominationPoolStakingProperties(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val sharedComputationScope: CoroutineScope,
    private val stakingOption: StakingOption,
): SingleStakingProperties {

    override fun availableBalance(asset: Asset): Balance {
        return asset.transferableInPlanks
    }

    override val recommendation: SingleStakingRecommendation = NominationPoolRecommendation()

    override val validationSystem: StartMultiStakingValidationSystem = ValidationSystem {
        // TODO
    }

    override suspend fun minStake(): Balance {
        return nominationPoolSharedComputation.minJoinBond(stakingOption.chain.id, sharedComputationScope)
    }
}
