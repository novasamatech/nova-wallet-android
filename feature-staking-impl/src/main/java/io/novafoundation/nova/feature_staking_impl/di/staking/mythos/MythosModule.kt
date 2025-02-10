package io.novafoundation.nova.feature_staking_impl.di.staking.mythos

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.validations.MythosClaimRewardsValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.validations.mythosClaimRewards
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.validations.MythosNoPendingRewardsValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.mythos.redeem.validations.RedeemMythosValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.mythos.redeem.validations.mythosRedeem
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.MythosMinimumDelegationValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.StartMythosStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.mythosStakingStart
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.validations.MythosReleaseRequestLimitNotReachedValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.validations.UnbondMythosValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.validations.mythosUnbond

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

    @Provides
    @FeatureScope
    fun provideUnbondValidationSystem(
        hasPendingRewardsValidationFactory: MythosNoPendingRewardsValidationFactory,
        releaseRequestLimitNotReachedValidation: MythosReleaseRequestLimitNotReachedValidationFactory
    ): UnbondMythosValidationSystem {
        return ValidationSystem.mythosUnbond(hasPendingRewardsValidationFactory, releaseRequestLimitNotReachedValidation)
    }

    @Provides
    @FeatureScope
    fun provideRedeemValidationSystem(): RedeemMythosValidationSystem {
        return ValidationSystem.mythosRedeem()
    }

    @Provides
    @FeatureScope
    fun provideClaimRewardsValidationSystem(): MythosClaimRewardsValidationSystem {
        return ValidationSystem.mythosClaimRewards()
    }
}
