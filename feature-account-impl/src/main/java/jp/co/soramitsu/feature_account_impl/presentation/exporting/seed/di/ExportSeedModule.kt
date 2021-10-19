package jp.co.soramitsu.feature_account_impl.presentation.exporting.seed.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import jp.co.soramitsu.common.di.viewmodel.ViewModelKey
import jp.co.soramitsu.common.di.viewmodel.ViewModelModule
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountInteractor
import jp.co.soramitsu.feature_account_impl.domain.account.export.seed.ExportSeedInteractor
import jp.co.soramitsu.feature_account_impl.presentation.AccountRouter
import jp.co.soramitsu.feature_account_impl.presentation.exporting.ExportPayload
import jp.co.soramitsu.feature_account_impl.presentation.exporting.seed.ExportSeedViewModel
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ExportSeedModule {

    @Provides
    @IntoMap
    @ViewModelKey(ExportSeedViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
        accountInteractor: AccountInteractor,
        interactor: ExportSeedInteractor,
        chainRegistry: ChainRegistry,
        payload: ExportPayload,
    ): ViewModel {
        return ExportSeedViewModel(
            router,
            interactor,
            resourceManager,
            accountInteractor,
            chainRegistry,
            payload
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ExportSeedViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ExportSeedViewModel::class.java)
    }
}
