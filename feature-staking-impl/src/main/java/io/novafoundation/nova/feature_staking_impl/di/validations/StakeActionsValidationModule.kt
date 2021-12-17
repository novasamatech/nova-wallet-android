package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.main.ManageStakeAction
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.MainStakingAccountRequiredValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationSystem

@Target(AnnotationTarget.FUNCTION)
@MapKey
annotation class StakeActionsValidationKey(val value: ManageStakeAction)

@Module
class StakeActionsValidationModule {

    @FeatureScope
    @Provides
    fun provideControllerValidation(
        stakingSharedState: StakingSharedState,
        accountRepository: AccountRepository
    ) = MainStakingAccountRequiredValidation(
        accountRepository,
        accountAddressExtractor = { it.stashState.controllerAddress },
        errorProducer = StakeActionsValidationFailure::ControllerRequired,
        sharedState = stakingSharedState
    )

    @FeatureScope
    @StakeActionsValidationKey(ManageStakeAction.REWARD_DESTINATION)
    @IntoMap
    @Provides
    fun provideRewardDestinationValidationSystem(
        controllerRequiredValidation: MainStakingAccountRequiredValidation,
    ) = StakeActionsValidationSystem(controllerRequiredValidation)
}
