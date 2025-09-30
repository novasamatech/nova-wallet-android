package io.novafoundation.nova.feature_multisig_operations.presentation.enterCall.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_multisig_operations.domain.details.MultisigOperationDetailsInteractor
import io.novafoundation.nova.feature_multisig_operations.domain.details.RealMultisigOperationDetailsInteractor
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.common.MultisigOperationPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.details.general.di.MultisigOperationDetailsModule.BindsModule
import io.novafoundation.nova.feature_multisig_operations.presentation.enterCall.MultisigOperationEnterCallViewModel

@Module(includes = [ViewModelModule::class, BindsModule::class])
class MultisigOperationEnterCallModule {

    @Module
    interface BindsModule {

        @Binds
        fun bindInteractor(real: RealMultisigOperationDetailsInteractor): MultisigOperationDetailsInteractor
    }

    @Provides
    @IntoMap
    @ViewModelKey(MultisigOperationEnterCallViewModel::class)
    fun provideViewModel(
        router: MultisigOperationsRouter,
        interactor: MultisigOperationDetailsInteractor,
        multisigOperationsService: MultisigPendingOperationsService,
        payload: MultisigOperationPayload,
        resourceManager: ResourceManager
    ): ViewModel {
        return MultisigOperationEnterCallViewModel(
            router = router,
            interactor = interactor,
            multisigOperationsService = multisigOperationsService,
            payload = payload,
            resourceManager = resourceManager,
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): MultisigOperationEnterCallViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(MultisigOperationEnterCallViewModel::class.java)
    }
}
