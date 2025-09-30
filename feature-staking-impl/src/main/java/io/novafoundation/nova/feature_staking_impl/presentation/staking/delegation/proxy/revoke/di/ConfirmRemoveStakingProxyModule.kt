package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.revoke.di

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.remove.RemoveStakingProxyInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.remove.RemoveStakingProxyValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.revoke.ConfirmRemoveStakingProxyPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.revoke.ConfirmRemoveStakingProxyViewModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState

@Module(includes = [ViewModelModule::class])
class ConfirmRemoveStakingProxyModule {
    @Provides
    @IntoMap
    @ViewModelKey(ConfirmRemoveStakingProxyViewModel::class)
    fun provideViewModule(
        router: StakingRouter,
        addressIconGenerator: AddressIconGenerator,
        payload: ConfirmRemoveStakingProxyPayload,
        accountRepository: AccountRepository,
        resourceManager: ResourceManager,
        externalActions: ExternalActions.Presentation,
        validationExecutor: ValidationExecutor,
        selectedAssetState: AnySelectedAssetOptionSharedState,
        assetUseCase: ArbitraryAssetUseCase,
        walletUiUseCase: WalletUiUseCase,
        removeStakingProxyInteractor: RemoveStakingProxyInteractor,
        removeStakingProxyValidationSystem: RemoveStakingProxyValidationSystem,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
    ): ViewModel {
        return ConfirmRemoveStakingProxyViewModel(
            router = router,
            addressIconGenerator = addressIconGenerator,
            payload = payload,
            accountRepository = accountRepository,
            resourceManager = resourceManager,
            externalActions = externalActions,
            validationExecutor = validationExecutor,
            selectedAssetState = selectedAssetState,
            assetUseCase = assetUseCase,
            walletUiUseCase = walletUiUseCase,
            removeStakingProxyInteractor = removeStakingProxyInteractor,
            removeStakingProxyValidationSystem = removeStakingProxyValidationSystem,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ConfirmRemoveStakingProxyViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmRemoveStakingProxyViewModel::class.java)
    }
}
