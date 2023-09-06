package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypePayload
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypeValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model.ValidatedStakingTypeDetails
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

interface StakingTypeDetailsProvider {

    val stakingType: Chain.Asset.StakingType

    val stakingTypeDetails: Flow<ValidatedStakingTypeDetails>

    val recommendationProvider: SingleStakingRecommendation

    fun getValidationSystem(): EditingStakingTypeValidationSystem

    suspend fun getValidationPayload(): EditingStakingTypePayload?
}
