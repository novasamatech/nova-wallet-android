package io.novafoundation.nova.feature_staking_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressCommunicator
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_proxy_api.di.ProxyFeatureApi
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdateSystem
import io.novafoundation.nova.feature_staking_impl.di.staking.UpdatersModule
import io.novafoundation.nova.feature_staking_impl.di.staking.dashboard.StakingDashboardModule
import io.novafoundation.nova.feature_staking_impl.di.staking.nominationPool.NominationPoolModule
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.ParachainStakingModule
import io.novafoundation.nova.feature_staking_impl.di.staking.stakingTypeDetails.StakingTypeDetailsModule
import io.novafoundation.nova.feature_staking_impl.di.staking.startMultiStaking.StartMultiStakingModule
import io.novafoundation.nova.feature_staking_impl.di.staking.unbond.StakingUnbondModule
import io.novafoundation.nova.feature_staking_impl.di.validations.AddStakingProxyValidationsModule
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.rewards.NominationPoolRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bagList.rebag.di.RebagComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm.di.ConfirmChangeValidatorsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm.nominations.di.ConfirmNominationsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.di.StakingDashboardComponent
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.di.MoreStakingOptionsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.di.NominationPoolsConfirmBondMoreComponent
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.setup.di.NominationPoolsSetupBondMoreComponent
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.claimRewards.di.NominationPoolsClaimRewardsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.redeem.di.NominationPoolsRedeemComponent
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.confirm.di.NominationPoolsConfirmUnbondComponent
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.setup.di.NominationPoolsSetupUnbondComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current.di.CurrentCollatorsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.search.di.SearchCollatorComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.di.SelectCollatorComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.di.SelectCollatorSettingsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.di.ParachainStakingRebondComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.redeem.di.ParachainStakingRedeemComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.di.ConfirmStartParachainStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.di.SetupStartParachainStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.di.ParachainStakingUnbondConfirmComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.setup.di.ParachainStakingUnbondComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.di.YieldBoostConfirmComponent
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.setup.di.SetupYieldBoostComponent
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.di.ConfirmPayoutComponent
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.detail.di.PayoutDetailsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.list.di.PayoutsListComponent
import io.novafoundation.nova.feature_staking_impl.presentation.period.di.StakingPeriodComponent
import io.novafoundation.nova.feature_staking_impl.presentation.pools.searchPool.di.SearchPoolComponent
import io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool.di.SelectPoolComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.di.ConfirmBondMoreComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select.di.SelectBondMoreComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.confirm.di.ConfirmSetControllerComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.set.di.SetControllerComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.confirm.di.ConfirmAddStakingProxyComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.set.di.AddStakingProxyComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.StakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm.di.ConfirmRebondComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.custom.di.CustomRebondComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.redeem.di.RedeemComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.di.ConfirmRewardDestinationComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.select.di.SelectRewardDestinationComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.di.ConfirmMultiStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.di.SetupStakingTypeComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.di.StartStakingLandingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.di.SetupAmountMultiStakingComponent
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
        StartMultiStakingModule::class,
        StakingTypeDetailsModule::class,
        AddStakingProxyValidationsModule::class
    ]
)
@FeatureScope
interface StakingFeatureComponent : StakingFeatureApi {

    fun dashboardComponentFactory(): StakingDashboardComponent.Factory

    fun moreStakingOptionsFactory(): MoreStakingOptionsComponent.Factory

    // start multi-staking

    fun startStakingLandingComponentFactory(): StartStakingLandingComponent.Factory

    fun setupAmountMultiStakingComponentFactory(): SetupAmountMultiStakingComponent.Factory

    fun setupStakingType(): SetupStakingTypeComponent.Factory

    fun confirmMultiStakingComponentFactory(): ConfirmMultiStakingComponent.Factory

    // relaychain staking

    fun searchCustomValidatorsComponentFactory(): SearchCustomValidatorsComponent.Factory

    fun customValidatorsSettingsComponentFactory(): CustomValidatorsSettingsComponent.Factory

    fun reviewCustomValidatorsComponentFactory(): ReviewCustomValidatorsComponent.Factory

    fun selectCustomValidatorsComponentFactory(): SelectCustomValidatorsComponent.Factory

    fun startChangeValidatorsComponentFactory(): StartChangeValidatorsComponent.Factory

    fun selectPoolComponentFactory(): SelectPoolComponent.Factory

    fun searchPoolComponentFactory(): SearchPoolComponent.Factory

    fun recommendedValidatorsComponentFactory(): RecommendedValidatorsComponent.Factory

    fun stakingComponentFactory(): StakingComponent.Factory

    fun stakingPeriodComponentFactory(): StakingPeriodComponent.Factory

    fun confirmStakingComponentFactory(): ConfirmChangeValidatorsComponent.Factory

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

    fun setStakingProxyFactory(): AddStakingProxyComponent.Factory

    fun confirmSetControllerFactory(): ConfirmSetControllerComponent.Factory

    fun confirmAddStakingProxyFactory(): ConfirmAddStakingProxyComponent.Factory

    fun rebondCustomFactory(): CustomRebondComponent.Factory

    fun currentValidatorsFactory(): CurrentValidatorsComponent.Factory

    fun selectRewardDestinationFactory(): SelectRewardDestinationComponent.Factory

    fun confirmRewardDestinationFactory(): ConfirmRewardDestinationComponent.Factory

    fun rebagComponentFractory(): RebagComponent.Factory

    // parachain staking

    fun startParachainStakingFactory(): SetupStartParachainStakingComponent.Factory

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

    // nomination pools

    fun nominationPoolsStakingSetupBondMore(): NominationPoolsSetupBondMoreComponent.Factory

    fun nominationPoolsStakingConfirmBondMore(): NominationPoolsConfirmBondMoreComponent.Factory

    fun nominationPoolsStakingSetupUnbond(): NominationPoolsSetupUnbondComponent.Factory

    fun nominationPoolsStakingConfirmUnbond(): NominationPoolsConfirmUnbondComponent.Factory

    fun nominationPoolsStakingRedeem(): NominationPoolsRedeemComponent.Factory

    fun nominationPoolsStakingClaimRewards(): NominationPoolsClaimRewardsComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: StakingRouter,

            @BindsInstance parachainStaking: ParachainStakingRouter,
            @BindsInstance selectCollatorInterScreenCommunicator: SelectCollatorInterScreenCommunicator,
            @BindsInstance selectCollatorSettingsInterScreenCommunicator: SelectCollatorSettingsInterScreenCommunicator,
            @BindsInstance selectAddressCommunicator: SelectAddressCommunicator,

            @BindsInstance nominationPoolsRouter: NominationPoolsRouter,

            @BindsInstance startMultiStakingRouter: StartMultiStakingRouter,
            @BindsInstance stakingDashboardRouter: StakingDashboardRouter,

            deps: StakingFeatureDependencies
        ): StakingFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class,
            RuntimeApi::class,
            AccountFeatureApi::class,
            ProxyFeatureApi::class,
            WalletFeatureApi::class,
            DAppFeatureApi::class
        ]
    )
    interface StakingFeatureDependenciesComponent : StakingFeatureDependencies

    val nominationPoolRewardCalculatorFactory: NominationPoolRewardCalculatorFactory

    val stakingUpdateSystem: StakingUpdateSystem

    val stakingSharedState: StakingSharedState
}
