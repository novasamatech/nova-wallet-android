package io.novafoundation.nova.feature_account_impl.presentation.importing.di

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.domain.common.AdvancedEncryptionSelectionStoreProvider
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.AccountNameChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl.AccountNameChooserFactory
import io.novafoundation.nova.feature_account_impl.presentation.importing.FileReader
import io.novafoundation.nova.feature_account_impl.presentation.importing.ImportAccountViewModel
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.ImportSourceFactory

@Module(includes = [ViewModelModule::class])
class ImportAccountModule {

    @Provides
    fun provideImportSourceFactory(
        addAccountInteractor: AddAccountInteractor,
        clipboardManager: ClipboardManager,
        advancedEncryptionSelectionStoreProvider: AdvancedEncryptionSelectionStoreProvider,
        fileReader: FileReader,
        advancedEncryptionInteractor: AdvancedEncryptionInteractor,
    ) = ImportSourceFactory(
        addAccountInteractor = addAccountInteractor,
        clipboardManager = clipboardManager,
        advancedEncryptionInteractor = advancedEncryptionInteractor,
        advancedEncryptionSelectionStoreProvider = advancedEncryptionSelectionStoreProvider,
        fileReader = fileReader
    )

    @Provides
    fun provideNameChooserMixinFactory(
        payload: ImportAccountPayload,
    ): MixinFactory<AccountNameChooserMixin.Presentation> {
        return AccountNameChooserFactory(payload.addAccountPayload)
    }

    @Provides
    @ScreenScope
    fun provideFileReader(context: Context) = FileReader(context)

    @Provides
    @IntoMap
    @ViewModelKey(ImportAccountViewModel::class)
    fun provideViewModel(
        interactor: AccountInteractor,
        router: AccountRouter,
        resourceManager: ResourceManager,
        accountNameChooserFactory: MixinFactory<AccountNameChooserMixin.Presentation>,
        importSourceFactory: ImportSourceFactory,
        payload: ImportAccountPayload,
    ): ViewModel {
        return ImportAccountViewModel(
            interactor,
            router,
            resourceManager,
            accountNameChooserFactory,
            payload,
            importSourceFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ImportAccountViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ImportAccountViewModel::class.java)
    }
}
