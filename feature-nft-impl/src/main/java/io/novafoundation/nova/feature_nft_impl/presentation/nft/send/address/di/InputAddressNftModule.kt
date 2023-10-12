package io.novafoundation.nova.feature_assets.presentation.send.amount.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_assets.presentation.send.amount.InputAddressNftViewModel
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.domain.nft.details.NftDetailsInteractor
import io.novafoundation.nova.feature_nft_impl.domain.nft.send.NftSendInteractor
import io.novafoundation.nova.feature_nft_impl.presentation.NftPayload
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class InputAddressNftModule {

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
    @ViewModelKey(InputAddressNftViewModel::class)
    fun provideViewModel(
        metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
        router: NftRouter,
        nftPayload: NftPayload,
        nftDetailsInteractor: NftDetailsInteractor,
        nftSendInteractor: NftSendInteractor,
        validationExecutor: ValidationExecutor,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        selectedAccountUseCase: SelectedAccountUseCase,
        addressInputMixinFactory: AddressInputMixinFactory,
        selectAddressCommunicator: SelectAddressCommunicator,
        externalActions: ExternalActions.Presentation,
        resourceManager: ResourceManager,
    ): ViewModel {
        return InputAddressNftViewModel(
            metaAccountGroupingInteractor = metaAccountGroupingInteractor,
            router = router,
            nftPayload = nftPayload,
            nftDetailsInteractor = nftDetailsInteractor,
            nftSendInteractor = nftSendInteractor,
            validationExecutor = validationExecutor,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            selectedAccountUseCase = selectedAccountUseCase,
            addressInputMixinFactory = addressInputMixinFactory,
            selectAddressRequester = selectAddressCommunicator,
            externalActions = externalActions,
            resourceManager = resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): InputAddressNftViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(InputAddressNftViewModel::class.java)
    }
}
