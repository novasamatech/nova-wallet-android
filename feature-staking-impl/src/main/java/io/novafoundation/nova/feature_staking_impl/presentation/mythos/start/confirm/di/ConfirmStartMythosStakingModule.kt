package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingBlockNumberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.StartMythosStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.StartMythosStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.StakingStartedDetectionService
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations.MythosStakingValidationFailureFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm.ConfirmStartMythosStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm.ConfirmStartMythosStakingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.common.di.StartParachainStakingModule
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2

@Module(includes = [ViewModelModule::class, StartParachainStakingModule::class])
class ConfirmStartMythosStakingModule {

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmStartMythosStakingViewModel::class)
    fun provideViewModel(
        mythosRouter: MythosStakingRouter,
        startStakingRouter: StartMultiStakingRouter,
        addressIconGenerator: AddressIconGenerator,
        selectedAccountUseCase: SelectedAccountUseCase,
        resourceManager: ResourceManager,
        feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
        externalActions: ExternalActions.Presentation,
        selectedAssetState: StakingSharedState,
        validationExecutor: ValidationExecutor,
        assetUseCase: AssetUseCase,
        payload: ConfirmStartMythosStakingPayload,
        stakingStartedDetectionService: StakingStartedDetectionService,
        mythosSharedComputation: MythosSharedComputation,
        walletUiUseCase: WalletUiUseCase,
        validationSystem: StartMythosStakingValidationSystem,
        stakingBlockNumberUseCase: StakingBlockNumberUseCase,
        mythosStakingValidationFailureFormatter: MythosStakingValidationFailureFormatter,
        interactor: StartMythosStakingInteractor,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return ConfirmStartMythosStakingViewModel(
            mythosRouter = mythosRouter,
            startStakingRouter = startStakingRouter,
            addressIconGenerator = addressIconGenerator,
            selectedAccountUseCase = selectedAccountUseCase,
            resourceManager = resourceManager,
            feeLoaderMixinV2Factory = feeLoaderMixinV2Factory,
            externalActions = externalActions,
            selectedAssetState = selectedAssetState,
            validationExecutor = validationExecutor,
            assetUseCase = assetUseCase,
            payload = payload,
            stakingStartedDetectionService = stakingStartedDetectionService,
            mythosSharedComputation = mythosSharedComputation,
            walletUiUseCase = walletUiUseCase,
            validationSystem = validationSystem,
            stakingBlockNumberUseCase = stakingBlockNumberUseCase,
            mythosStakingValidationFailureFormatter = mythosStakingValidationFailureFormatter,
            interactor = interactor,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper,
            amountFormatter = amountFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ConfirmStartMythosStakingViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmStartMythosStakingViewModel::class.java)
    }
}
