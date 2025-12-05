package io.novafoundation.nova.feature_account_impl.presentation.seedScan.di

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
import io.novafoundation.nova.feature_account_impl.domain.scanSeed.RealScanSeedInteractor
import io.novafoundation.nova.feature_account_impl.domain.scanSeed.ScanSeedInteractor
import io.novafoundation.nova.feature_account_impl.domain.utils.SecretQrFormat
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.seedScan.ScanSeedCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.seedScan.ScanSeedViewModel

@Module(includes = [ViewModelModule::class])
class ScanSeedModule {

    @Provides
    fun provideInteractor(): ScanSeedInteractor {
        return RealScanSeedInteractor(SecretQrFormat())
    }

    @Provides
    fun providePermissionAsker(
        permissionsAskerFactory: PermissionsAskerFactory,
        fragment: Fragment,
        router: AccountRouter
    ) = permissionsAskerFactory.createReturnable(fragment, router)

    @Provides
    @IntoMap
    @ViewModelKey(ScanSeedViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        permissionsAsker: PermissionsAsker.Presentation,
        resourceManager: ResourceManager,
        interactor: ScanSeedInteractor,
        scanSeedResponder: ScanSeedCommunicator
    ): ViewModel {
        return ScanSeedViewModel(
            router = router,
            permissionsAsker = permissionsAsker,
            interactor = interactor,
            resourceManager = resourceManager,
            scanSeedResponder = scanSeedResponder
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ScanSeedViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ScanSeedViewModel::class.java)
    }
}
