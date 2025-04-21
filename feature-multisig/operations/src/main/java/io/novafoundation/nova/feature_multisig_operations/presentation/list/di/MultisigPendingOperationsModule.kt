package io.novafoundation.nova.feature_multisig_operations.presentation.list.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.common.MultisigOperationFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.list.MultisigPendingOperationsViewModel

@Module(includes = [ViewModelModule::class])
class MultisigPendingOperationsModule {

    @Provides
    @IntoMap
    @ViewModelKey(MultisigPendingOperationsViewModel::class)
    fun provideViewModel(
        discoveryService: MultisigPendingOperationsService,
        router: MultisigOperationsRouter,
        resourceManager: ResourceManager,
        operationFormatter: MultisigOperationFormatter
    ): ViewModel {
        return MultisigPendingOperationsViewModel(
            discoveryService = discoveryService,
            router = router,
            resourceManager = resourceManager,
            operationFormatter = operationFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): MultisigPendingOperationsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(MultisigPendingOperationsViewModel::class.java)
    }
}
