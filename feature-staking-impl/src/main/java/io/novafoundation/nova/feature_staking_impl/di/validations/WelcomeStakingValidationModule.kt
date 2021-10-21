package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingMaxNominatorsValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationFailure

@Module
class WelcomeStakingValidationModule {

    @Provides
    @FeatureScope
    fun provideMaxNominatorsReachedValidation(
        stakingSharedState: StakingSharedState,
        stakingRepository: StakingRepository
    ) = WelcomeStakingMaxNominatorsValidation(
        stakingRepository = stakingRepository,
        errorProducer = { WelcomeStakingValidationFailure.MAX_NOMINATORS_REACHED },
        isAlreadyNominating = { false },
        sharedState = stakingSharedState
    )

    @Provides
    @FeatureScope
    fun provideSetupStakingValidationSystem(
        maxNominatorsReachedValidation: WelcomeStakingMaxNominatorsValidation
    ) = ValidationSystem(
        CompositeValidation(
            listOf(
                maxNominatorsReachedValidation,
            )
        )
    )
}
