package io.novafoundation.nova.feature_nft_impl.presentation.nft.list.di

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.domain.nft.list.NftListInteractor
import io.novafoundation.nova.feature_nft_impl.presentation.nft.list.NftListViewModel
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module(includes = [ViewModelModule::class])
class NftListModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        accountRepository: AccountRepository,
        nftRepository: NftRepository,
        tokenRepository: TokenRepository
    ) = NftListInteractor(
        accountRepository = accountRepository,
        tokenRepository = tokenRepository,
        nftRepository = nftRepository
    )

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): NftListViewModel {
        return ViewModelProvider(fragment, factory).get(NftListViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(NftListViewModel::class)
    fun provideViewModel(
        router: NftRouter,
        resourceManager: ResourceManager,
        interactor: NftListInteractor,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return NftListViewModel(
            router = router,
            resourceManager = resourceManager,
            interactor = interactor,
            amountFormatter = amountFormatter
        )
    }
}
