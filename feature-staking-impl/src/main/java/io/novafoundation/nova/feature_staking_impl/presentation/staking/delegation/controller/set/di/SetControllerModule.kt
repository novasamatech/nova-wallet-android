package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.set.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.controller.ControllerInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller.SetControllerValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.set.SetControllerViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class SetControllerModule {
    @Provides
    @IntoMap
    @ViewModelKey(SetControllerViewModel::class)
    fun provideViewModel(
        interactor: ControllerInteractor,
        stakingInteractor: StakingInteractor,
        addressIconGenerator: AddressIconGenerator,
        router: StakingRouter,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        externalActions: ExternalActions.Presentation,
        appLinksProvider: AppLinksProvider,
        resourceManager: ResourceManager,
        addressDisplayUseCase: AddressDisplayUseCase,
        validationExecutor: ValidationExecutor,
        validationSystem: SetControllerValidationSystem,
        selectedAssetState: StakingSharedState,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
    ): ViewModel {
        return SetControllerViewModel(
            interactor = interactor,
            stakingInteractor = stakingInteractor,
            addressIconGenerator = addressIconGenerator,
            router = router,
            feeLoaderMixin = feeLoaderMixin,
            externalActions = externalActions,
            appLinksProvider = appLinksProvider,
            resourceManager = resourceManager,
            addressDisplayUseCase = addressDisplayUseCase,
            validationExecutor = validationExecutor,
            validationSystem = validationSystem,
            selectedAssetState = selectedAssetState,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SetControllerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SetControllerViewModel::class.java)
    }
}
