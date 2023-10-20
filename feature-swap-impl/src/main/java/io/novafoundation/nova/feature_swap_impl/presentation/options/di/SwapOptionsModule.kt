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
import io.novafoundation.nova.common.validation.InputValidationMixinFactory
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_impl.domain.slippage.SlippageRepository
import io.novafoundation.nova.feature_swap_impl.domain.validation.slippage.SlippageValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.slippage.SlippageValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.slippage.SlippageValidationSystem
import io.novafoundation.nova.feature_swap_impl.domain.validation.slippage.getSlippageValidationSystem
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.options.SwapOptionsViewModel

@Module(includes = [ViewModelModule::class])
class SwapOptionsModule {

    @Provides
    fun validationSystem(slippageRepository: SlippageRepository): SlippageValidationSystem {
        return getSlippageValidationSystem(slippageRepository)
    }

    @Provides
    fun provideInputValidationMixinFactory(
        validationSystem: SlippageValidationSystem
    ): InputValidationMixinFactory<SlippageValidationPayload, SlippageValidationFailure> {
        return InputValidationMixinFactory(validationSystem)
    }

    @Provides
    @IntoMap
    @ViewModelKey(SwapOptionsViewModel::class)
    fun provideViewModel(
        swapRouter: SwapRouter,
        resourceManager: ResourceManager,
        swapSettingsStateProvider: SwapSettingsStateProvider,
        inputValidationMixinFactory: InputValidationMixinFactory<SlippageValidationPayload, SlippageValidationFailure>,
        slippageRepository: SlippageRepository
    ): ViewModel {
        return SwapOptionsViewModel(
            swapRouter,
            resourceManager,
            swapSettingsStateProvider,
            inputValidationMixinFactory,
            slippageRepository
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
