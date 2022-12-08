package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.setupStaking

@Module
class SetupStakingValidationsModule {

    @Provides
    @FeatureScope
    fun provideSetupStakingValidationSystem(
        stakingRepository: StakingRepository,
        sharedState: StakingSharedState
    ): SetupStakingValidationSystem {
        return ValidationSystem.setupStaking(stakingRepository, sharedState)
    }
}
