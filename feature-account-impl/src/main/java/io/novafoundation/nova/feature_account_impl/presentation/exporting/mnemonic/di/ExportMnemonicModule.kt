package io.novafoundation.nova.feature_account_impl.presentation.exporting.mnemonic.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.export.mnemonic.ExportMnemonicInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.mnemonic.ExportMnemonicViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

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
        advancedEncryptionCommunicator: AdvancedEncryptionCommunicator,
        payload: ExportPayload
    ): ViewModel {
        return ExportMnemonicViewModel(
            router,
            interactor,
            advancedEncryptionCommunicator,
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
