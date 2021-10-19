package jp.co.soramitsu.feature_account_impl.presentation.exporting.mnemonic.di

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
import jp.co.soramitsu.feature_account_impl.domain.account.export.mnemonic.ExportMnemonicInteractor
import jp.co.soramitsu.feature_account_impl.presentation.AccountRouter
import jp.co.soramitsu.feature_account_impl.presentation.exporting.ExportPayload
import jp.co.soramitsu.feature_account_impl.presentation.exporting.mnemonic.ExportMnemonicViewModel
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ExportMnemonicModule {

    @Provides
    @IntoMap
    @ViewModelKey(ExportMnemonicViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
        accountInteractor: AccountInteractor,
        chainRegistry: ChainRegistry,
        interactor: ExportMnemonicInteractor,
        payload: ExportPayload
    ): ViewModel {
        return ExportMnemonicViewModel(
            router,
            interactor,
            resourceManager,
            chainRegistry,
            accountInteractor,
            payload
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ExportMnemonicViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ExportMnemonicViewModel::class.java)
    }
}
