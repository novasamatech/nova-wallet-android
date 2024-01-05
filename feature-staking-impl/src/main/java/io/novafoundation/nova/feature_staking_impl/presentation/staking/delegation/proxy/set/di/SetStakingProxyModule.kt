package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.set.di

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.proxy.AddProxyInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressForTransactionRequester
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.controller.ControllerInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller.SetControllerValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.set.SetControllerViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.set.SetStakingProxyViewModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState

@Module(includes = [ViewModelModule::class])
class SetStakingProxyModule {

    @Provides
    @IntoMap
    @ViewModelKey(SetStakingProxyViewModel::class)
    fun provideViewModel(
        addressInputMixinFactory: AddressInputMixinFactory,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        selectedAssetState: AnySelectedAssetOptionSharedState,
        externalActions: ExternalActions.Presentation,
        interactor: StakingInteractor,
        accountRepository: AccountRepository,
        assetUseCase: ArbitraryAssetUseCase,
        resourceManager: ResourceManager,
        selectAddressCommunicator: SelectAddressCommunicator,
        addProxyInteractor: AddProxyInteractor,
        validationExecutor: ValidationExecutor
    ): ViewModel {
        return SetStakingProxyViewModel(
            addressInputMixinFactory = addressInputMixinFactory,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            selectedAssetState = selectedAssetState,
            externalActions = externalActions,
            interactor = interactor,
            accountRepository = accountRepository,
            assetUseCase = assetUseCase,
            resourceManager = resourceManager,
            selectAddressRequester = selectAddressCommunicator,
            addProxyInteractor = addProxyInteractor,
            validationExecutor = validationExecutor
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SetStakingProxyViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SetStakingProxyViewModel::class.java)
    }
}
