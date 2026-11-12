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
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation.SlippageFieldValidatorFactory
import io.novafoundation.nova.feature_swap_impl.presentation.options.SwapOptionsViewModel
import io.novafoundation.nova.feature_swap_impl.presentation.options.SwapSettingsSheetViewModel
import io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation.SlippageFieldValidator

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
        slippageAlertMixinFactory: SlippageAlertMixinFactory,
        payload: SlippageSettingsPayload?,
        slippageSettingsCommunicator: SlippageSettingsCommunicator
    ): ViewModel {
        return SwapOptionsViewModel(
            swapRouter,
            resourceManager,
            swapSettingsStateProvider,
            slippageFieldValidatorFactory,
            swapInteractor,
            descriptionBottomSheetLauncher,
            slippageAlertMixinFactory,
            payload,
            slippageSettingsCommunicator
        )
    }

    @Provides
    fun provideSheetSlippageFieldValidator(
        payload: SlippageSettingsPayload?,
        factory: SlippageFieldValidatorFactory
    ): SlippageFieldValidator = factory.create(requireNotNull(payload).config())

    @Provides
    @IntoMap
    @ViewModelKey(SwapSettingsSheetViewModel::class)
    fun provideSheetViewModel(
        swapRouter: SwapRouter,
        resourceManager: ResourceManager,
        slippageFieldValidator: SlippageFieldValidator,
        slippageAlertMixinFactory: SlippageAlertMixinFactory,
        payload: SlippageSettingsPayload?,
        slippageSettingsCommunicator: SlippageSettingsCommunicator
    ): ViewModel {
        return SwapSettingsSheetViewModel(
            swapRouter,
            resourceManager,
            slippageFieldValidator,
            slippageAlertMixinFactory,
            requireNotNull(payload),
            slippageSettingsCommunicator
        )
    }

    @Provides
    fun provideSheetViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SwapSettingsSheetViewModel = ViewModelProvider(fragment, viewModelFactory).get(SwapSettingsSheetViewModel::class.java)

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SwapOptionsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SwapOptionsViewModel::class.java)
    }
}
