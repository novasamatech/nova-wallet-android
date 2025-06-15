package io.novafoundation.nova.feature_swap_impl.presentation.execution.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapFlowScopeAggregator
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.details.SwapConfirmationDetailsFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapStateStoreProvider
import io.novafoundation.nova.feature_swap_impl.presentation.execution.SwapExecutionViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SwapExecutionModule {

    @Provides
    @IntoMap
    @ViewModelKey(SwapExecutionViewModel::class)
    fun provideViewModel(
        swapStateStoreProvider: SwapStateStoreProvider,
        swapInteractor: SwapInteractor,
        resourceManager: ResourceManager,
        router: SwapRouter,
        chainRegistry: ChainRegistry,
        confirmationDetailsFormatter: SwapConfirmationDetailsFormatter,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
        swapFlowScopeAggregator: SwapFlowScopeAggregator,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    ): ViewModel {
        return SwapExecutionViewModel(
            swapStateStoreProvider = swapStateStoreProvider,
            swapInteractor = swapInteractor,
            resourceManager = resourceManager,
            router = router,
            chainRegistry = chainRegistry,
            confirmationDetailsFormatter = confirmationDetailsFormatter,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            descriptionBottomSheetLauncher = descriptionBottomSheetLauncher,
            swapFlowScopeAggregator = swapFlowScopeAggregator,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SwapExecutionViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SwapExecutionViewModel::class.java)
    }
}
