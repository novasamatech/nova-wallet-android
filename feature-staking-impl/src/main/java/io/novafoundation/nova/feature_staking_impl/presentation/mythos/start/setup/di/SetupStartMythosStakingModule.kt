package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.setup.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingBlockNumberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosDelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations.MythosCollatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.StartMythosStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.StartMythosStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.SelectMythosInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.MythosCollatorFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations.MythosStakingValidationFailureFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.setup.SetupStartMythosStakingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.setup.rewards.MythosStakingRewardsComponentFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2

@Module(includes = [ViewModelModule::class])
class SetupStartMythosStakingModule {

    @Provides
    @IntoMap
    @ViewModelKey(SetupStartMythosStakingViewModel::class)
    fun provideViewModel(
        router: MythosStakingRouter,
        rewardsComponentFactory: MythosStakingRewardsComponentFactory,
        assetUseCase: AssetUseCase,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        collatorRecommendatorFactory: MythosCollatorRecommendatorFactory,
        mythosDelegatorStateUseCase: MythosDelegatorStateUseCase,
        selectedAssetState: StakingSharedState,
        mythosSharedComputation: MythosSharedComputation,
        mythosCollatorFormatter: MythosCollatorFormatter,
        interactor: StartMythosStakingInteractor,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        selectCollatorInterScreenCommunicator: SelectMythosInterScreenCommunicator,
        validationSystem: StartMythosStakingValidationSystem,
        blockNumberUseCase: StakingBlockNumberUseCase,
        validationFailureFormatter: MythosStakingValidationFailureFormatter,
    ): ViewModel {
        return SetupStartMythosStakingViewModel(
            router = router,
            rewardsComponentFactory = rewardsComponentFactory,
            assetUseCase = assetUseCase,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            feeLoaderMixinV2Factory = feeLoaderMixinV2Factory,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            collatorRecommendatorFactory = collatorRecommendatorFactory,
            mythosDelegatorStateUseCase = mythosDelegatorStateUseCase,
            selectedAssetState = selectedAssetState,
            mythosSharedComputation = mythosSharedComputation,
            mythosCollatorFormatter = mythosCollatorFormatter,
            interactor = interactor,
            amountChooserMixinFactory = amountChooserMixinFactory,
            selectCollatorInterScreenRequester = selectCollatorInterScreenCommunicator,
            validationSystem = validationSystem,
            mythosStakingValidationFailureFormatter = validationFailureFormatter,
            stakingBlockNumberUseCase = blockNumberUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SetupStartMythosStakingViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SetupStartMythosStakingViewModel::class.java)
    }
}
