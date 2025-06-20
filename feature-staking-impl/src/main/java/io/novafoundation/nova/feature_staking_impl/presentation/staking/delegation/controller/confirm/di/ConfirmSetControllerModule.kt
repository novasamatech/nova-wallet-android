package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.confirm.di

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
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.controller.ControllerInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller.SetControllerValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.confirm.ConfirmSetControllerPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.confirm.ConfirmSetControllerViewModel

@Module(includes = [ViewModelModule::class])
class ConfirmSetControllerModule {
    @Provides
    @IntoMap
    @ViewModelKey(ConfirmSetControllerViewModel::class)
    fun provideViewModule(
        router: StakingRouter,
        controllerInteractor: ControllerInteractor,
        addressIconGenerator: AddressIconGenerator,
        payload: ConfirmSetControllerPayload,
        interactor: StakingInteractor,
        resourceManager: ResourceManager,
        externalActions: ExternalActions.Presentation,
        validationExecutor: ValidationExecutor,
        validationSystem: SetControllerValidationSystem,
        singleAssetSharedState: StakingSharedState,
        walletUiUseCase: WalletUiUseCase,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
    ): ViewModel {
        return ConfirmSetControllerViewModel(
            router = router,
            controllerInteractor = controllerInteractor,
            addressIconGenerator = addressIconGenerator,
            payload = payload,
            interactor = interactor,
            resourceManager = resourceManager,
            externalActions = externalActions,
            validationExecutor = validationExecutor,
            validationSystem = validationSystem,
            selectedAssetState = singleAssetSharedState,
            walletUiUseCase = walletUiUseCase,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ConfirmSetControllerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmSetControllerViewModel::class.java)
    }
}
