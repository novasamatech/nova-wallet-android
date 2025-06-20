package io.novafoundation.nova.feature_swap_impl.presentation.confirmation.di

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
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapFlowScopeAggregator
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.details.SwapConfirmationDetailsFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapStateStoreProvider
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.SwapConfirmationViewModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SwapConfirmationModule {

    @Provides
    @IntoMap
    @ViewModelKey(SwapConfirmationViewModel::class)
    fun provideViewModel(
        swapRouter: SwapRouter,
        swapInteractor: SwapInteractor,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        walletUiUseCase: WalletUiUseCase,
        slippageAlertMixinFactory: SlippageAlertMixinFactory,
        addressIconGenerator: AddressIconGenerator,
        validationExecutor: ValidationExecutor,
        tokenRepository: TokenRepository,
        externalActions: ExternalActions.Presentation,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
        assetUseCase: ArbitraryAssetUseCase,
        maxActionProviderFactory: MaxActionProviderFactory,
        swapStateStoreProvider: SwapStateStoreProvider,
        confirmationDetailsFormatter: SwapConfirmationDetailsFormatter,
        resourceManager: ResourceManager,
        swapFlowScopeAggregator: SwapFlowScopeAggregator,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
    ): ViewModel {
        return SwapConfirmationViewModel(
            swapRouter = swapRouter,
            swapInteractor = swapInteractor,
            accountRepository = accountRepository,
            chainRegistry = chainRegistry,
            walletUiUseCase = walletUiUseCase,
            slippageAlertMixinFactory = slippageAlertMixinFactory,
            addressIconGenerator = addressIconGenerator,
            validationExecutor = validationExecutor,
            tokenRepository = tokenRepository,
            externalActions = externalActions,
            swapStateStoreProvider = swapStateStoreProvider,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            descriptionBottomSheetLauncher = descriptionBottomSheetLauncher,
            arbitraryAssetUseCase = assetUseCase,
            maxActionProviderFactory = maxActionProviderFactory,
            swapConfirmationDetailsFormatter = confirmationDetailsFormatter,
            resourceManager = resourceManager,
            swapFlowScopeAggregator = swapFlowScopeAggregator,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SwapConfirmationViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SwapConfirmationViewModel::class.java)
    }
}
