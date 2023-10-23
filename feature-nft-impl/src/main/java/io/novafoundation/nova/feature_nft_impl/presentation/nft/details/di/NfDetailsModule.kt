package io.novafoundation.nova.feature_nft_impl.presentation.nft.details.di

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
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.domain.nft.details.NftDetailsInteractor
import io.novafoundation.nova.feature_nft_impl.presentation.nft.details.NftDetailsViewModel
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository

@Module(includes = [ViewModelModule::class])
class NfDetailsModule {

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
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): NftDetailsViewModel {
        return ViewModelProvider(fragment, factory).get(NftDetailsViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(NftDetailsViewModel::class)
    fun provideViewModel(
        router: NftRouter,
        resourceManager: ResourceManager,
        interactor: NftDetailsInteractor,
        nftIdentifier: String,
        accountExternalActions: ExternalActions.Presentation,
        addressIconGenerator: AddressIconGenerator,
        addressDisplayUseCase: AddressDisplayUseCase
    ): ViewModel {
        return NftDetailsViewModel(
            router = router,
            resourceManager = resourceManager,
            interactor = interactor,
            nftIdentifier = nftIdentifier,
            externalActionsDelegate = accountExternalActions,
            addressIconGenerator = addressIconGenerator,
            addressDisplayUseCase = addressDisplayUseCase
        )
    }
}
