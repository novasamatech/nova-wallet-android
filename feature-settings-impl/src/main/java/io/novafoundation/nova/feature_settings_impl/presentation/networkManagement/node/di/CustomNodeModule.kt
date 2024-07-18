package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.node.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.CustomNodeInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.node.CustomNodePayload
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.node.CustomNodeViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class CustomNodeModule {

    @Provides
    @IntoMap
    @ViewModelKey(CustomNodeViewModel::class)
    fun provideViewModel(
        router: SettingsRouter,
        resourceManager: ResourceManager,
        payload: CustomNodePayload,
        customNodeInteractor: CustomNodeInteractor,
        validationExecutor: ValidationExecutor,
        chainRegistry: ChainRegistry
    ): ViewModel {
        return CustomNodeViewModel(
            router,
            resourceManager,
            payload,
            customNodeInteractor,
            validationExecutor,
            chainRegistry
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): CustomNodeViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(CustomNodeViewModel::class.java)
    }
}
