package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.scan.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan.RealScanImportParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan.ScanImportParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerStartPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.scan.ScanImportParitySignerViewModel

@Module(includes = [ViewModelModule::class])
class ScanImportParitySignerModule {

    @Provides
    fun provideInteractor(): ScanImportParitySignerInteractor = RealScanImportParitySignerInteractor()

    @Provides
    fun providePermissionAsker(
        permissionsAskerFactory: PermissionsAskerFactory,
        fragment: Fragment,
        router: AccountRouter
    ) = permissionsAskerFactory.create(fragment, router)

    @Provides
    @IntoMap
    @ViewModelKey(ScanImportParitySignerViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        permissionsAsker: PermissionsAsker.Presentation,
        resourceManager: ResourceManager,
        interactor: ScanImportParitySignerInteractor,
        payload: ParitySignerStartPayload,
    ): ViewModel {
        return ScanImportParitySignerViewModel(
            router = router,
            permissionsAsker = permissionsAsker,
            interactor = interactor,
            resourceManager = resourceManager,
            payload = payload
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ScanImportParitySignerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ScanImportParitySignerViewModel::class.java)
    }
}
