package io.novafoundation.nova.feature_nft_impl.presentation.nft.send.confirm.di

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
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.domain.nft.details.NftDetailsInteractor
import io.novafoundation.nova.feature_nft_impl.domain.nft.send.NftSendInteractor
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.NftTransferDraft
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.confirm.ConfirmNftSendViewModel
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ConfirmNftSendModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        nftRepository: NftRepository,
        tokenRepository: TokenRepository
    ) = NftDetailsInteractor(
        tokenRepository = tokenRepository,
        nftRepository = nftRepository
    )

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmNftSendViewModel::class)
    fun provideViewModel(
        nftSendInteractor: NftSendInteractor,
        router: NftRouter,
        addressIconGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        chainRegistry: ChainRegistry,
        selectedAccountUseCase: SelectedAccountUseCase,
        addressDisplayUseCase: AddressDisplayUseCase,
        validationExecutor: ValidationExecutor,
        walletUiUseCase: WalletUiUseCase,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        transferDraft: NftTransferDraft,
        resourceManager: ResourceManager,
    ): ViewModel {
        return ConfirmNftSendViewModel(
            nftSendInteractor = nftSendInteractor,
            router = router,
            addressIconGenerator = addressIconGenerator,
            externalActions = externalActions,
            chainRegistry = chainRegistry,
            selectedAccountUseCase = selectedAccountUseCase,
            addressDisplayUseCase = addressDisplayUseCase,
            validationExecutor = validationExecutor,
            walletUiUseCase = walletUiUseCase,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            transferDraft = transferDraft,
            resourceManager = resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ConfirmNftSendViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmNftSendViewModel::class.java)
    }
}
