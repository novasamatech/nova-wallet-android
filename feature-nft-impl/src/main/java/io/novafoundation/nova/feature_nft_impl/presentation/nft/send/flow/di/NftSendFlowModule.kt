package io.novafoundation.nova.feature_assets.presentation.send.flow.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_assets.presentation.send.flow.NftSendFlowViewModel
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.domain.nft.search.NftSearchInteractor

@Module(includes = [ViewModelModule::class])
class NftSendFlowModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): NftSendFlowViewModel {
        return ViewModelProvider(fragment, factory).get(NftSendFlowViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(NftSendFlowViewModel::class)
    fun provideViewModel(
        router: NftRouter,
        nftSearchInteractor: NftSearchInteractor
    ): ViewModel {
        return NftSendFlowViewModel(
            router = router,
            interactor = nftSearchInteractor
        )
    }
}
