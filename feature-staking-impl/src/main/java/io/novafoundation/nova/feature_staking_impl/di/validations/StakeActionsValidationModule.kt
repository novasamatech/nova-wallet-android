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
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.BALANCE_CONTROLLER_IS_NOT_ALLOWED
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.BALANCE_REQUIRED_CONTROLLER
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.BALANCE_REQUIRED_STASH_META_ACCOUNT
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.ControllerAccountIsNotAllowedValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.MainStakingMetaAccountRequiredValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.MainStakingUnlockingLimitValidation
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_ADD_PROXY
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_PROXIES
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
    ) = MainStakingMetaAccountRequiredValidation(
        accountRepository,
        accountAddressExtractor = { it.stashState.controllerAddress },
        errorProducer = StakeActionsValidationFailure::ControllerRequired,
        sharedState = stakingSharedState
    )

    @FeatureScope
    @Named(BALANCE_REQUIRED_STASH_META_ACCOUNT)
    @Provides
    fun provideStashValidation(
        stakingSharedState: StakingSharedState,
        accountRepository: AccountRepository
    ) = MainStakingMetaAccountRequiredValidation(
        accountRepository,
        accountAddressExtractor = { it.stashState.stashAddress },
        errorProducer = StakeActionsValidationFailure::StashRequired,
        sharedState = stakingSharedState
    )

    @FeatureScope
    @Named(BALANCE_CONTROLLER_IS_NOT_ALLOWED)
    @Provides
    fun provideControllerNotAllowedValidation(
        stakingSharedState: StakingSharedState,
        accountRepository: AccountRepository
    ) = ControllerAccountIsNotAllowedValidation(
        accountRepository,
        stakingState = { it.stashState },
        errorProducer = StakeActionsValidationFailure::StashRequiredToManageProxies,
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
        controllerRequiredValidation: MainStakingMetaAccountRequiredValidation,
    ) = StakeActionsValidationSystem(
        CompositeValidation(
            validations = listOf(
                controllerRequiredValidation
            )
        )
    )

    @FeatureScope
    @Named(SYSTEM_MANAGE_PROXIES)
    @Provides
    fun provideManageProxiesValidationSystem(
        @Named(BALANCE_CONTROLLER_IS_NOT_ALLOWED)
        controllerAccountIsNotAllowedValidation: ControllerAccountIsNotAllowedValidation,
    ) = StakeActionsValidationSystem(controllerAccountIsNotAllowedValidation)

    @FeatureScope
    @Named(SYSTEM_ADD_PROXY)
    @Provides
    fun provideAddProxiesValidationSystem(
        @Named(BALANCE_CONTROLLER_IS_NOT_ALLOWED)
        controllerAccountIsNotAllowedValidation: ControllerAccountIsNotAllowedValidation,
    ) = StakeActionsValidationSystem(controllerAccountIsNotAllowedValidation)

    @FeatureScope
    @Named(SYSTEM_MANAGE_STAKING_BOND_MORE)
    @Provides
    fun provideBondMoreValidationSystem(
        @Named(BALANCE_REQUIRED_STASH_META_ACCOUNT)
        stashRequiredValidation: MainStakingMetaAccountRequiredValidation,
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
        controllerRequiredValidation: MainStakingMetaAccountRequiredValidation,
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
        controllerRequiredValidation: MainStakingMetaAccountRequiredValidation
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
        @Named(BALANCE_REQUIRED_STASH_META_ACCOUNT)
        stashRequiredValidation: MainStakingMetaAccountRequiredValidation
    ): StakeActionsValidationSystem = ValidationSystem {
        validate(stashRequiredValidation)
    }

    @FeatureScope
    @Named(SYSTEM_MANAGE_REWARD_DESTINATION)
    @Provides
    fun provideRewardDestinationValidationSystemToMap(
        @Named(BALANCE_REQUIRED_CONTROLLER)
        controllerRequiredValidation: MainStakingMetaAccountRequiredValidation,
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

    @FeatureScope
    @StakeActionsValidationKey(SYSTEM_MANAGE_PROXIES)
    @IntoMap
    @Binds
    fun provideManageProxyValidationSystemToMap(
        @Named(SYSTEM_MANAGE_PROXIES) system: StakeActionsValidationSystem,
    ): StakeActionsValidationSystem

    @FeatureScope
    @StakeActionsValidationKey(SYSTEM_ADD_PROXY)
    @IntoMap
    @Binds
    fun provideAddProxyValidationSystemToMap(
        @Named(SYSTEM_ADD_PROXY) system: StakeActionsValidationSystem,
    ): StakeActionsValidationSystem
}
