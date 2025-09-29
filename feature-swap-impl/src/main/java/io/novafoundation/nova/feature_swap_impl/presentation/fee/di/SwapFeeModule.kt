package io.novafoundation.nova.feature_swap_impl.presentation.fee.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapStateStoreProvider
import io.novafoundation.nova.feature_swap_impl.presentation.fee.SwapFeeViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SwapFeeModule {

    @Provides
    @IntoMap
    @ViewModelKey(SwapFeeViewModel::class)
    fun provideViewModel(
        swapInteractor: SwapInteractor,
        chainRegistry: ChainRegistry,
        resourceManager: ResourceManager,
        swapStateStoreProvider: SwapStateStoreProvider,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return SwapFeeViewModel(
            swapInteractor = swapInteractor,
            chainRegistry = chainRegistry,
            resourceManager = resourceManager,
            swapStateStoreProvider = swapStateStoreProvider,
            amountFormatter = amountFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SwapFeeViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SwapFeeViewModel::class.java)
    }
}
