package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.changeValidators

@Module
class SetupStakingValidationsModule {

    @Provides
    @FeatureScope
    fun provideSetupStakingValidationSystem(
        stakingRepository: StakingRepository,
        stakingSharedComputation: StakingSharedComputation,
    ): SetupStakingValidationSystem {
        return ValidationSystem.changeValidators(stakingRepository, stakingSharedComputation)
    }
}
