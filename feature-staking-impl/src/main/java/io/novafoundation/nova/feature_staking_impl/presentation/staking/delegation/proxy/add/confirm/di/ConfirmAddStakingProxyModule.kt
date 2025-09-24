package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.confirm.di

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
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.AddStakingProxyInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.AddStakingProxyValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.confirm.ConfirmAddStakingProxyPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.confirm.ConfirmAddStakingProxyViewModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module(includes = [ViewModelModule::class])
class ConfirmAddStakingProxyModule {
    @Provides
    @IntoMap
    @ViewModelKey(ConfirmAddStakingProxyViewModel::class)
    fun provideViewModule(
        router: StakingRouter,
        addressIconGenerator: AddressIconGenerator,
        payload: ConfirmAddStakingProxyPayload,
        accountRepository: AccountRepository,
        resourceManager: ResourceManager,
        externalActions: ExternalActions.Presentation,
        validationExecutor: ValidationExecutor,
        selectedAssetState: AnySelectedAssetOptionSharedState,
        assetUseCase: ArbitraryAssetUseCase,
        addStakingProxyValidationSystem: AddStakingProxyValidationSystem,
        addStakingProxyRepository: AddStakingProxyInteractor,
        walletUiUseCase: WalletUiUseCase,
        descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return ConfirmAddStakingProxyViewModel(
            router = router,
            addressIconGenerator = addressIconGenerator,
            payload = payload,
            accountRepository = accountRepository,
            resourceManager = resourceManager,
            externalActions = externalActions,
            validationExecutor = validationExecutor,
            selectedAssetState = selectedAssetState,
            assetUseCase = assetUseCase,
            addStakingProxyValidationSystem = addStakingProxyValidationSystem,
            addStakingProxyInteractor = addStakingProxyRepository,
            walletUiUseCase = walletUiUseCase,
            descriptionBottomSheetLauncher = descriptionBottomSheetLauncher,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper,
            amountFormatter = amountFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ConfirmAddStakingProxyViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmAddStakingProxyViewModel::class.java)
    }
}
