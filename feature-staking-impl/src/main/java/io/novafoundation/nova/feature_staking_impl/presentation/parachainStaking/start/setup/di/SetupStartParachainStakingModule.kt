package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.StartParachainStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.common.di.StartParachainStakingModule
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.hints.ConfirmStartParachainStakingHintsMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.StartParachainStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.StartParachainStakingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.ParachainStakingRewardsComponentFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory

@Module(includes = [ViewModelModule::class, StartParachainStakingModule::class])
class SetupStartParachainStakingModule {

    @Provides
    @ScreenScope
    fun provideRewardsComponentFactory(
        rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
        singleAssetSharedState: StakingSharedState,
        resourceManager: ResourceManager,
    ) = ParachainStakingRewardsComponentFactory(rewardCalculatorFactory, singleAssetSharedState, resourceManager)

    @Provides
    @IntoMap
    @ViewModelKey(StartParachainStakingViewModel::class)
    fun provideViewModel(
        router: ParachainStakingRouter,
        selectCollatorInterScreenCommunicator: SelectCollatorInterScreenCommunicator,
        interactor: StartParachainStakingInteractor,
        assetUseCase: AssetUseCase,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        rewardsComponentFactory: ParachainStakingRewardsComponentFactory,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        validationSystem: StartParachainStakingValidationSystem,
        addressIconGenerator: AddressIconGenerator,
        delegatorStateUseCase: DelegatorStateUseCase,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        hintsMixinFactory: ConfirmStartParachainStakingHintsMixinFactory,
        collatorsUseCase: CollatorsUseCase,
        selectedAssetState: StakingSharedState,
        collatorRecommendatorFactory: CollatorRecommendatorFactory,
        payload: StartParachainStakingPayload,
        maxActionProviderFactory: MaxActionProviderFactory,
    ): ViewModel {
        return StartParachainStakingViewModel(
            router = router,
            selectCollatorInterScreenRequester = selectCollatorInterScreenCommunicator,
            interactor = interactor,
            rewardsComponentFactory = rewardsComponentFactory,
            assetUseCase = assetUseCase,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            amountChooserMixinFactory = amountChooserMixinFactory,
            addressIconGenerator = addressIconGenerator,
            validationSystem = validationSystem,
            delegatorStateUseCase = delegatorStateUseCase,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            collatorsUseCase = collatorsUseCase,
            hintsMixinFactory = hintsMixinFactory,
            selectedAssetState = selectedAssetState,
            collatorRecommendatorFactory = collatorRecommendatorFactory,
            payload = payload,
            maxActionProviderFactory = maxActionProviderFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): StartParachainStakingViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StartParachainStakingViewModel::class.java)
    }
}
