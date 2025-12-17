package io.novafoundation.nova.feature_assets.presentation.transaction.detail.swap.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.swap.SwapDetailViewModel
import io.novafoundation.nova.feature_swap_api.presentation.formatters.SwapRateFormatter
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryTokenUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module(includes = [ViewModelModule::class])
class SwapDetailModule {

    @Provides
    @IntoMap
    @ViewModelKey(SwapDetailViewModel::class)
    fun provideViewModel(
        router: AssetsRouter,
        addressIconGenerator: AddressIconGenerator,
        chainRegistry: ChainRegistry,
        operation: OperationParcelizeModel.Swap,
        externalActions: ExternalActions.Presentation,
        arbitraryTokenUseCase: ArbitraryTokenUseCase,
        walletUiUseCase: WalletUiUseCase,
        swapRateFormatter: SwapRateFormatter,
        assetIconProvider: AssetIconProvider,
        descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return SwapDetailViewModel(
            router = router,
            addressIconGenerator = addressIconGenerator,
            chainRegistry = chainRegistry,
            operation = operation,
            externalActions = externalActions,
            arbitraryTokenUseCase = arbitraryTokenUseCase,
            walletUiUseCase = walletUiUseCase,
            swapRateFormatter = swapRateFormatter,
            descriptionBottomSheetLauncher = descriptionBottomSheetLauncher,
            assetIconProvider = assetIconProvider,
            amountFormatter = amountFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SwapDetailViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SwapDetailViewModel::class.java)
    }
}
