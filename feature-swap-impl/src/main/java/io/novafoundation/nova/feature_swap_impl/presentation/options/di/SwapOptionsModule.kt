package io.novafoundation.nova.feature_swap_impl.presentation.options.di

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
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.fieldValidation.SlippageFieldValidatorFactory
import io.novafoundation.nova.feature_swap_impl.presentation.options.SwapOptionsViewModel

@Module(includes = [ViewModelModule::class])
class SwapOptionsModule {

    @Provides
    fun provideSlippageFieldValidatorFactory(
        resourceManager: ResourceManager
    ): SlippageFieldValidatorFactory {
        return SlippageFieldValidatorFactory(resourceManager)
    }

    @Provides
    @IntoMap
    @ViewModelKey(SwapOptionsViewModel::class)
    fun provideViewModel(
        swapRouter: SwapRouter,
        resourceManager: ResourceManager,
        swapSettingsStateProvider: SwapSettingsStateProvider,
        slippageFieldValidatorFactory: SlippageFieldValidatorFactory,
        swapInteractor: SwapInteractor,
        descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
        slippageAlertMixinFactory: SlippageAlertMixinFactory
    ): ViewModel {
        return SwapOptionsViewModel(
            swapRouter,
            resourceManager,
            swapSettingsStateProvider,
            slippageFieldValidatorFactory,
            swapInteractor,
            descriptionBottomSheetLauncher,
            slippageAlertMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SwapOptionsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SwapOptionsViewModel::class.java)
    }
}
