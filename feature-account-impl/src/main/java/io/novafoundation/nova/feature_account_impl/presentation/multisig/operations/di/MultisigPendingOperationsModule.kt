package io.novafoundation.nova.feature_account_impl.presentation.multisig.operations.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.condition.ConditionMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.export.mnemonic.ExportMnemonicInteractor
import io.novafoundation.nova.feature_account_impl.domain.common.AdvancedEncryptionSelectionStoreProvider
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup.BackupMnemonicPayload
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup.BackupMnemonicViewModel
import io.novafoundation.nova.feature_account_impl.presentation.multisig.operations.MultisigPendingOperationsViewModel

@Module(includes = [ViewModelModule::class])
class MultisigPendingOperationsModule {

    @Provides
    @IntoMap
    @ViewModelKey(MultisigPendingOperationsViewModel::class)
    fun provideViewModel(
        discoveryService: MultisigPendingOperationsService,
        router: AccountRouter,
        resourceManager: ResourceManager,
    ): ViewModel {
        return MultisigPendingOperationsViewModel(
            discoveryService = discoveryService,
            router = router,
            resourceManager = resourceManager
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
