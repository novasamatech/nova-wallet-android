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
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation.SlippageFieldValidatorFactory
import io.novafoundation.nova.feature_swap_impl.presentation.options.SlippageSettingsViewModel

@Module(includes = [ViewModelModule::class])
class SlippageSettingsModule {

    @Provides
    fun provideSlippageFieldValidatorFactory(
        resourceManager: ResourceManager
    ): SlippageFieldValidatorFactory {
        return SlippageFieldValidatorFactory(resourceManager)
    }

    @Provides
    @IntoMap
    @ViewModelKey(SlippageSettingsViewModel::class)
    fun provideViewModel(
        swapRouter: SwapRouter,
        resourceManager: ResourceManager,
        slippageFieldValidatorFactory: SlippageFieldValidatorFactory,
        slippageAlertMixinFactory: SlippageAlertMixinFactory,
        slippageSettingsCommunicator: SlippageSettingsCommunicator,
        payload: SlippageSettingsPayload
    ): ViewModel {
        return SlippageSettingsViewModel(
            swapRouter,
            resourceManager,
            slippageFieldValidatorFactory,
            slippageAlertMixinFactory,
            slippageSettingsCommunicator,
            payload
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SlippageSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SlippageSettingsViewModel::class.java)
    }
}
