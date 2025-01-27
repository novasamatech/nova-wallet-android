package io.novafoundation.nova.feature_staking_impl.di.staking.mythos

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.MythosMinimumDelegationValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.MythosNoPendingRewardsValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.StartMythosStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.mythosStakingStart

@Module(includes = [MythosBindsModule::class])
class MythosModule {

    @Provides
    @FeatureScope
    fun provideStartStakingValidationSystem(
        minimumDelegationValidationFactory: MythosMinimumDelegationValidationFactory,
        hasPendingRewardsValidationFactory: MythosNoPendingRewardsValidationFactory
    ): StartMythosStakingValidationSystem {
        return ValidationSystem.mythosStakingStart(minimumDelegationValidationFactory, hasPendingRewardsValidationFactory)
    }
}
