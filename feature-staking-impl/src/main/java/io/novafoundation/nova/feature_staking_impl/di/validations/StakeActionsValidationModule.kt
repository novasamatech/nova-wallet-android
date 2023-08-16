package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.BALANCE_REQUIRED_CONTROLLER
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.BALANCE_REQUIRED_STASH
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.MainStakingAccountRequiredValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.MainStakingUnlockingLimitValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_REWARD_DESTINATION
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_BOND_MORE
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_REBAG
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_REBOND
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_REDEEM
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_UNBOND
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationSystem
import javax.inject.Named

@Target(AnnotationTarget.FUNCTION)
@MapKey
annotation class StakeActionsValidationKey(val value: String)

@Module
class StakeActionsValidationsModule {

    // --------- validations ----------

    @FeatureScope
    @Named(BALANCE_REQUIRED_CONTROLLER)
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
    @Named(BALANCE_REQUIRED_STASH)
    @Provides
    fun provideStashValidation(
        stakingSharedState: StakingSharedState,
        accountRepository: AccountRepository
    ) = MainStakingAccountRequiredValidation(
        accountRepository,
        accountAddressExtractor = { it.stashState.stashAddress },
        errorProducer = StakeActionsValidationFailure::StashRequired,
        sharedState = stakingSharedState
    )

    @FeatureScope
    @Provides
    fun provideUnbondingLimitValidation(
        stakingRepository: StakingRepository,
    ) = MainStakingUnlockingLimitValidation(
        stakingRepository,
        stashStateProducer = { it.stashState },
        errorProducer = StakeActionsValidationFailure::UnbondingRequestLimitReached
    )

    // --------- validation systems ----------

    @FeatureScope
    @Named(SYSTEM_MANAGE_STAKING_REDEEM)
    @Provides
    fun provideRedeemValidationSystem(
        @Named(BALANCE_REQUIRED_CONTROLLER)
        controllerRequiredValidation: MainStakingAccountRequiredValidation,
    ) = StakeActionsValidationSystem(
        CompositeValidation(
            validations = listOf(
                controllerRequiredValidation
            )
        )
    )

    @FeatureScope
    @Named(SYSTEM_MANAGE_STAKING_BOND_MORE)
    @Provides
    fun provideBondMoreValidationSystem(
        @Named(BALANCE_REQUIRED_STASH)
        stashRequiredValidation: MainStakingAccountRequiredValidation,
    ) = StakeActionsValidationSystem(
        CompositeValidation(
            validations = listOf(
                stashRequiredValidation,
            )
        )
    )

    @FeatureScope
    @Named(SYSTEM_MANAGE_STAKING_UNBOND)
    @Provides
    fun provideUnbondValidationSystem(
        @Named(BALANCE_REQUIRED_CONTROLLER)
        controllerRequiredValidation: MainStakingAccountRequiredValidation,
        balanceUnlockingLimitValidation: MainStakingUnlockingLimitValidation
    ) = StakeActionsValidationSystem(
        CompositeValidation(
            validations = listOf(
                controllerRequiredValidation,
                balanceUnlockingLimitValidation
            )
        )
    )

    @FeatureScope
    @Named(SYSTEM_MANAGE_STAKING_REBOND)
    @Provides
    fun provideRebondValidationSystem(
        @Named(BALANCE_REQUIRED_CONTROLLER)
        controllerRequiredValidation: MainStakingAccountRequiredValidation
    ) = StakeActionsValidationSystem(
        CompositeValidation(
            validations = listOf(
                controllerRequiredValidation
            )
        )
    )

    @FeatureScope
    @Named(SYSTEM_MANAGE_STAKING_REBAG)
    @Provides
    fun provideRebagValidationSystem(
        @Named(BALANCE_REQUIRED_CONTROLLER)
        controllerRequiredValidation: MainStakingAccountRequiredValidation
    ): StakeActionsValidationSystem = ValidationSystem {
        validate(controllerRequiredValidation)
    }

    @FeatureScope
    @Named(SYSTEM_MANAGE_REWARD_DESTINATION)
    @Provides
    fun provideRewardDestinationValidationSystemToMap(
        @Named(BALANCE_REQUIRED_CONTROLLER)
        controllerRequiredValidation: MainStakingAccountRequiredValidation,
    ): StakeActionsValidationSystem = StakeActionsValidationSystem(controllerRequiredValidation)
}

@Module(includes = [StakeActionsValidationsModule::class])
interface StakeActionsValidationModule {

    @FeatureScope
    @StakeActionsValidationKey(SYSTEM_MANAGE_REWARD_DESTINATION)
    @IntoMap
    @Binds
    fun provideRewardDestinationValidationSystemToMap(
        @Named(SYSTEM_MANAGE_REWARD_DESTINATION) system: StakeActionsValidationSystem,
    ): StakeActionsValidationSystem

    @FeatureScope
    @StakeActionsValidationKey(SYSTEM_MANAGE_STAKING_BOND_MORE)
    @IntoMap
    @Binds
    fun provideBondMoreValidationSystemToMap(
        @Named(SYSTEM_MANAGE_STAKING_BOND_MORE) system: StakeActionsValidationSystem,
    ): StakeActionsValidationSystem

    @FeatureScope
    @StakeActionsValidationKey(SYSTEM_MANAGE_STAKING_UNBOND)
    @IntoMap
    @Binds
    fun provideUnbondValidationSystemToMap(
        @Named(SYSTEM_MANAGE_STAKING_UNBOND) system: StakeActionsValidationSystem,
    ): StakeActionsValidationSystem
}
