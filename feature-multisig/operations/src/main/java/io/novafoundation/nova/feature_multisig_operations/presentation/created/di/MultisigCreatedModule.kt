package io.novafoundation.nova.feature_multisig_operations.presentation.created.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.created.MultisigCreatedPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.created.MultisigCreatedViewModel

@Module(includes = [ViewModelModule::class])
class MultisigCreatedModule {

    @Provides
    @IntoMap
    @ViewModelKey(MultisigCreatedViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        router: MultisigOperationsRouter,
        payload: MultisigCreatedPayload
    ): ViewModel {
        return MultisigCreatedViewModel(resourceManager, router, payload)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): MultisigCreatedViewModel {
        return ViewModelProvider(
            fragment,
            viewModelFactory
        ).get(MultisigCreatedViewModel::class.java)
    }
}
