package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types

import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.feature_staking_impl.data.createStakingOption
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model.ValidatedStakingTypeDetails
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class CompoundStakingTypeDetailsProvidersFactory(
    private val factories: Map<StakingTypeGroup, StakingTypeDetailsProviderFactory>
) {

    suspend fun create(
        coroutineScope: CoroutineScope,
        chainWithAsset: ChainWithAsset,
        availableStakingTypes: List<Chain.Asset.StakingType>
    ): CompoundStakingTypeDetailsProviders {
        val providers = chainWithAsset.asset.staking.mapNotNull { stakingType ->
            val supportedFactory = factories[stakingType.group()]
            supportedFactory?.create(createStakingOption(chainWithAsset, stakingType), coroutineScope, availableStakingTypes)
        }

        return CompoundStakingTypeDetailsProviders(providers)
    }
}

class CompoundStakingTypeDetailsProviders(private val providers: List<StakingTypeDetailsProvider>) {

    fun getStakingTypeDetails(): Flow<List<ValidatedStakingTypeDetails>> {
        return providers.map { it.stakingTypeDetails }
            .combine()
    }

    fun getRecommendationProvider(stakingType: Chain.Asset.StakingType): SingleStakingRecommendation {
        return providers.first { it.stakingType == stakingType }
            .recommendationProvider
    }

}
