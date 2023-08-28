package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.createStakingOption
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypeFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypeValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model.EditableStakingType
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface StakingTypeDetailsProviderFactory {

    suspend fun create(
        stakingOption: StakingOption,
        coroutineScope: CoroutineScope,
        availableStakingTypes: List<Chain.Asset.StakingType>
    ): StakingTypeDetailsProvider
}

class CompoundStakingTypeDetailsProvidersFactory(
    private val factories: Map<StakingTypeGroup, StakingTypeDetailsProviderFactory>
) {

    suspend fun create(
        coroutineScope: CoroutineScope,
        chainWithAsset: ChainWithAsset,
        availableStakingTypes: List<Chain.Asset.StakingType>
    ): List<StakingTypeDetailsProvider> {
        return chainWithAsset.asset.staking.mapNotNull { stakingType ->
            val supportedFactory = factories[stakingType.group()]
            supportedFactory?.create(createStakingOption(chainWithAsset, stakingType), coroutineScope, availableStakingTypes)
        }
    }
}

interface StakingTypeDetailsProvider {

    val stakingType: Chain.Asset.StakingType

    val stakingTypeDetails: Flow<EditableStakingType>

    val recommendationProvider: SingleStakingRecommendation

    fun getValidationSystem(): EditingStakingTypeValidationSystem
}
