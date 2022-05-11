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
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.RealStartParachainStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.StartParachainStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.minimumDelegation
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.StartParachainStakingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.RealParachainStakingRewardsComponentFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class StartParachainStakingModule {

    @Provides
    @ScreenScope
    fun provideValidationSystem(
        stakingConstantsRepository: ParachainStakingConstantsRepository
    ): StartParachainStakingValidationSystem = ValidationSystem {
        minimumDelegation(stakingConstantsRepository)

        sufficientBalance(
            fee = { it.fee },
            amount = { it.amount },
            available = { it.asset.transferable },
            error = { StartParachainStakingValidationFailure.NotEnoughBalanceToPayFees }
        )
    }

    @Provides
    @ScreenScope
    fun provideInteractor(
        extrinsicService: ExtrinsicService,
        chainRegistry: ChainRegistry,
        singleAssetSharedState: StakingSharedState,
        collatorProvider: CollatorProvider,
        stakingConstantsRepository: ParachainStakingConstantsRepository,
    ): StartParachainStakingInteractor = RealStartParachainStakingInteractor(
        extrinsicService,
        chainRegistry,
        singleAssetSharedState,
        collatorProvider,
        stakingConstantsRepository
    )

    @Provides
    @ScreenScope
    fun provideRewardsComponentFactory(
        rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
        singleAssetSharedState: StakingSharedState,
        resourceManager: ResourceManager,
    ) = RealParachainStakingRewardsComponentFactory(rewardCalculatorFactory, singleAssetSharedState, resourceManager)

    @Provides
    @IntoMap
    @ViewModelKey(StartParachainStakingViewModel::class)
    fun provideViewModel(
        router: StakingRouter,
        interactor: StartParachainStakingInteractor,
        assetUseCase: AssetUseCase,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        rewardsComponentFactory: RealParachainStakingRewardsComponentFactory,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        validationSystem: StartParachainStakingValidationSystem,
        singleAssetSharedState: StakingSharedState,
        addressIconGenerator: AddressIconGenerator,
    ): ViewModel {
        return StartParachainStakingViewModel(
            router = router,
            interactor = interactor,
            rewardsComponentFactory = rewardsComponentFactory,
            assetUseCase = assetUseCase,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            feeLoaderMixin = feeLoaderMixin,
            amountChooserMixinFactory = amountChooserMixinFactory,
            singleAssetSharedState = singleAssetSharedState,
            addressIconGenerator = addressIconGenerator,
            validationSystem = validationSystem
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
