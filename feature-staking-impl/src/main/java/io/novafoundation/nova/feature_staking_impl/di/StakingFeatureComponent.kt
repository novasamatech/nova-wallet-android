package io.novafoundation.nova.feature_staking_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.di.staking.UpdatersModule
import io.novafoundation.nova.feature_staking_impl.di.staking.dashboard.StakingDashboardModule
import io.novafoundation.nova.feature_staking_impl.di.staking.nominationPool.NominationPoolModule
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.ParachainStakingModule
import io.novafoundation.nova.feature_staking_impl.di.staking.unbond.StakingUnbondModule
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bagList.rebag.di.RebagComponent
import io.novafoundation.nova.feature_staking_impl.presentation.confirm.di.ConfirmStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.confirm.nominations.di.ConfirmNominationsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.di.StakingDashboardComponent
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.di.MoreStakingOptionsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current.di.CurrentCollatorsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.search.di.SearchCollatorComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.di.SelectCollatorComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.di.SelectCollatorSettingsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.di.ParachainStakingRebondComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.redeem.di.ParachainStakingRedeemComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.di.ConfirmStartParachainStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.di.StartParachainStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.di.ParachainStakingUnbondConfirmComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.setup.di.ParachainStakingUnbondComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.di.YieldBoostConfirmComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.setup.di.SetupYieldBoostComponent
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.di.ConfirmPayoutComponent
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.detail.di.PayoutDetailsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.list.di.PayoutsListComponent
import io.novafoundation.nova.feature_staking_impl.presentation.period.di.StakingPeriodComponent
import io.novafoundation.nova.feature_staking_impl.presentation.setup.di.SetupStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.di.ConfirmBondMoreComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select.di.SelectBondMoreComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.controller.confirm.di.ConfirmSetControllerComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.controller.set.di.SetControllerComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.di.StartStakingLandingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.StakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm.di.ConfirmRebondComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.custom.di.CustomRebondComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.redeem.di.RedeemComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.di.ConfirmRewardDestinationComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.select.di.SelectRewardDestinationComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm.di.ConfirmUnbondComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.select.di.SelectUnbondComponent
import io.novafoundation.nova.feature_staking_impl.presentation.story.di.StoryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.di.ReviewCustomValidatorsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.search.di.SearchCustomValidatorsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.select.di.SelectCustomValidatorsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.settings.di.CustomValidatorsSettingsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.recommended.di.RecommendedValidatorsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.start.di.StartChangeValidatorsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.current.di.CurrentValidatorsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.di.ValidatorDetailsComponent
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        StakingFeatureDependencies::class
    ],
    modules = [
        StakingFeatureModule::class,
        UpdatersModule::class,
        StakingValidationModule::class,
        StakingUnbondModule::class,
        ParachainStakingModule::class,
        NominationPoolModule::class,
        StakingDashboardModule::class,
    ]
)
@FeatureScope
interface StakingFeatureComponent : StakingFeatureApi {

    fun dashboardComponentFactory(): StakingDashboardComponent.Factory

    fun moreStakingOptionsFactory(): MoreStakingOptionsComponent.Factory

    // relaychain staking

    fun searchCustomValidatorsComponentFactory(): SearchCustomValidatorsComponent.Factory

    fun customValidatorsSettingsComponentFactory(): CustomValidatorsSettingsComponent.Factory

    fun reviewCustomValidatorsComponentFactory(): ReviewCustomValidatorsComponent.Factory

    fun selectCustomValidatorsComponentFactory(): SelectCustomValidatorsComponent.Factory

    fun startChangeValidatorsComponentFactory(): StartChangeValidatorsComponent.Factory

    fun recommendedValidatorsComponentFactory(): RecommendedValidatorsComponent.Factory

    fun startStakingLandingComponentFactory(): StartStakingLandingComponent.Factory

    fun stakingComponentFactory(): StakingComponent.Factory

    fun stakingPeriodComponentFactory(): StakingPeriodComponent.Factory

    fun setupStakingComponentFactory(): SetupStakingComponent.Factory

    fun confirmStakingComponentFactory(): ConfirmStakingComponent.Factory

    fun confirmNominationsComponentFactory(): ConfirmNominationsComponent.Factory

    fun validatorDetailsComponentFactory(): ValidatorDetailsComponent.Factory

    fun storyComponentFactory(): StoryComponent.Factory

    fun payoutsListFactory(): PayoutsListComponent.Factory

    fun payoutDetailsFactory(): PayoutDetailsComponent.Factory

    fun confirmPayoutFactory(): ConfirmPayoutComponent.Factory

    fun selectBondMoreFactory(): SelectBondMoreComponent.Factory

    fun confirmBondMoreFactory(): ConfirmBondMoreComponent.Factory

    fun selectUnbondFactory(): SelectUnbondComponent.Factory

    fun confirmUnbondFactory(): ConfirmUnbondComponent.Factory

    fun redeemFactory(): RedeemComponent.Factory

    fun confirmRebondFactory(): ConfirmRebondComponent.Factory

    fun setControllerFactory(): SetControllerComponent.Factory

    fun confirmSetControllerFactory(): ConfirmSetControllerComponent.Factory

    fun rebondCustomFactory(): CustomRebondComponent.Factory

    fun currentValidatorsFactory(): CurrentValidatorsComponent.Factory

    fun selectRewardDestinationFactory(): SelectRewardDestinationComponent.Factory

    fun confirmRewardDestinationFactory(): ConfirmRewardDestinationComponent.Factory

    fun rebagComponentFractory(): RebagComponent.Factory

    // parachain staking

    fun startParachainStakingFactory(): StartParachainStakingComponent.Factory

    fun confirmStartParachainStakingFactory(): ConfirmStartParachainStakingComponent.Factory

    fun selectCollatorFactory(): SelectCollatorComponent.Factory

    fun selectCollatorSettingsFactory(): SelectCollatorSettingsComponent.Factory

    fun searchCollatorFactory(): SearchCollatorComponent.Factory

    fun currentCollatorsFactory(): CurrentCollatorsComponent.Factory

    fun parachainStakingUnbondSetupFactory(): ParachainStakingUnbondComponent.Factory

    fun parachainStakingUnbondConfirmFactory(): ParachainStakingUnbondConfirmComponent.Factory

    fun parachainStakingRedeemFactory(): ParachainStakingRedeemComponent.Factory

    fun parachainStakingRebondFactory(): ParachainStakingRebondComponent.Factory

    fun setupYieldBoostComponentFactory(): SetupYieldBoostComponent.Factory

    fun confirmYieldBoostComponentFactory(): YieldBoostConfirmComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: StakingRouter,

            @BindsInstance parachainStaking: ParachainStakingRouter,
            @BindsInstance selectCollatorInterScreenCommunicator: SelectCollatorInterScreenCommunicator,
            @BindsInstance selectCollatorSettingsInterScreenCommunicator: SelectCollatorSettingsInterScreenCommunicator,

            deps: StakingFeatureDependencies
        ): StakingFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class,
            RuntimeApi::class,
            AccountFeatureApi::class,
            WalletFeatureApi::class,
            DAppFeatureApi::class
        ]
    )
    interface StakingFeatureDependenciesComponent : StakingFeatureDependencies
}
