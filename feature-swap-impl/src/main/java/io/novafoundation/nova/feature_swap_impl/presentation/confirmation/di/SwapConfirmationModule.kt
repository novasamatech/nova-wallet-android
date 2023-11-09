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
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.PriceImpactFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.SwapRateFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload.SwapConfirmationPayload
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.SwapConfirmationViewModel
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload.SwapConfirmationPayloadFormatter
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_swap_impl.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SwapConfirmationModule {

    @Provides
    @IntoMap
    @ViewModelKey(SwapConfirmationViewModel::class)
    fun provideViewModel(
        swapRouter: SwapRouter,
        swapInteractor: SwapInteractor,
        resourceManager: ResourceManager,
        swapConfirmationPayload: SwapConfirmationPayload,
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        swapRateFormatter: SwapRateFormatter,
        priceImpactFormatter: PriceImpactFormatter,
        walletUiUseCase: WalletUiUseCase,
        slippageAlertMixinFactory: SlippageAlertMixinFactory,
        addressIconGenerator: AddressIconGenerator,
        validationExecutor: ValidationExecutor,
        tokenRepository: TokenRepository,
        externalActions: ExternalActions.Presentation,
        swapConfirmationPayloadFormatter: SwapConfirmationPayloadFormatter,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
        assetUseCase: ArbitraryAssetUseCase,
        maxActionProviderFactory: MaxActionProviderFactory
    ): ViewModel {
        return SwapConfirmationViewModel(
            swapRouter,
            swapInteractor,
            resourceManager,
            swapConfirmationPayload,
            walletRepository,
            accountRepository,
            chainRegistry,
            swapRateFormatter,
            priceImpactFormatter,
            walletUiUseCase,
            slippageAlertMixinFactory,
            addressIconGenerator,
            validationExecutor,
            tokenRepository,
            externalActions,
            swapConfirmationPayloadFormatter,
            feeLoaderMixinFactory,
            descriptionBottomSheetLauncher,
            assetUseCase,
            maxActionProviderFactory
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
