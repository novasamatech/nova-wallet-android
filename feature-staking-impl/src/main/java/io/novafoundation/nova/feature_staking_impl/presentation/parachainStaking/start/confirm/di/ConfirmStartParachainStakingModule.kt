package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.di

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.StartParachainStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.ParachainStakingHintsUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.ConfirmStartParachainStakingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.hints.ConfirmStartParachainStakingHintsMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class ConfirmStartParachainStakingModule {

    @Provides
    @ScreenScope
    fun provideConfirmStartParachainStakingHintsMixinFactory(
        stakingHintsUseCase: ParachainStakingHintsUseCase,
        resourceManager: ResourceManager
    ): ConfirmStartParachainStakingHintsMixinFactory {
        return ConfirmStartParachainStakingHintsMixinFactory(stakingHintsUseCase, resourceManager)
    }

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmStartParachainStakingViewModel::class)
    fun provideViewModel(
        router: ParachainStakingRouter,
        startStakingRouter: StartMultiStakingRouter,
        addressIconGenerator: AddressIconGenerator,
        selectedAccountUseCase: SelectedAccountUseCase,
        resourceManager: ResourceManager,
        validationSystem: StartParachainStakingValidationSystem,
        collatorsUseCase: CollatorsUseCase,
        validationExecutor: ValidationExecutor,
        assetUseCase: AssetUseCase,
        interactor: StartParachainStakingInteractor,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        externalActions: ExternalActions.Presentation,
        selectedAssetState: StakingSharedState,
        walletUiUseCase: WalletUiUseCase,
        payload: ConfirmStartParachainStakingPayload,
        hintsMixinFactory: ConfirmStartParachainStakingHintsMixinFactory,
        delegatorStateUseCase: DelegatorStateUseCase
    ): ViewModel {
        return ConfirmStartParachainStakingViewModel(
            parachainStakingRouter = router,
            addressIconGenerator = addressIconGenerator,
            selectedAccountUseCase = selectedAccountUseCase,
            resourceManager = resourceManager,
            validationSystem = validationSystem,
            interactor = interactor,
            feeLoaderMixin = feeLoaderMixin,
            externalActions = externalActions,
            selectedAssetState = selectedAssetState,
            validationExecutor = validationExecutor,
            assetUseCase = assetUseCase,
            walletUiUseCase = walletUiUseCase,
            payload = payload,
            hintsMixinFactory = hintsMixinFactory,
            collatorsUseCase = collatorsUseCase,
            delegatorStateUseCase = delegatorStateUseCase,
            startStakingRouter = startStakingRouter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ConfirmStartParachainStakingViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmStartParachainStakingViewModel::class.java)
    }
}
