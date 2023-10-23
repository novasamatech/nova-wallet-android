package io.novafoundation.nova.feature_assets.presentation.receive.flow.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_assets.presentation.receive.flow.NftReceiveFlowViewModel
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.domain.nft.chains.NftChainsInteractor

@Module(includes = [ViewModelModule::class])
class NftReceiveFlowModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): NftReceiveFlowViewModel {
        return ViewModelProvider(fragment, factory).get(NftReceiveFlowViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(NftReceiveFlowViewModel::class)
    fun provideViewModel(
        router: NftRouter,
        nftChainsInteractor: NftChainsInteractor
    ): ViewModel {
        return NftReceiveFlowViewModel(
            router = router,
            nftChainsInteractor = nftChainsInteractor
        )
    }
}
