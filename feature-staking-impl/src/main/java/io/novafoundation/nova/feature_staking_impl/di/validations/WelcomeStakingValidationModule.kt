package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.validation.hasChainAccount
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingMaxNominatorsValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationFailure.NoRelayChainAccount
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationSystem

@Module
class WelcomeStakingValidationModule {

    @Provides
    @FeatureScope
    fun provideMaxNominatorsReachedValidation(
        stakingRepository: StakingRepository
    ) = WelcomeStakingMaxNominatorsValidation(
        stakingRepository = stakingRepository,
        errorProducer = { WelcomeStakingValidationFailure.MaxNominatorsReached },
        isAlreadyNominating = { false },
        chainId = { it.chain.id }
    )

    @Provides
    @FeatureScope
    fun provideSetupStakingValidationSystem(
        maxNominatorsReachedValidation: WelcomeStakingMaxNominatorsValidation
    ): WelcomeStakingValidationSystem = ValidationSystem {
        validate(maxNominatorsReachedValidation)

        hasChainAccount(
            chain = { it.chain },
            metaAccount = { it.metaAccount },
            error = ::NoRelayChainAccount
        )
    }
}
