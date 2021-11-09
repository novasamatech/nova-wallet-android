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
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.AccountNameChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.CryptoTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.ForcedChainMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl.AccountNameChooserFactory
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl.CryptoTypeChooserFactory
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl.ForcedChainMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.importing.FileReader
import io.novafoundation.nova.feature_account_impl.presentation.importing.ImportAccountViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ImportAccountModule {

    @Provides
    @ScreenScope
    fun provideCryptoChooserMixinFactory(
        interactor: AccountInteractor,
        addAccountPayload: AddAccountPayload,
        resourceManager: ResourceManager,
    ): MixinFactory<CryptoTypeChooserMixin> {
        return CryptoTypeChooserFactory(
            interactor,
            addAccountPayload,
            resourceManager
        )
    }

    @Provides
    @ScreenScope
    fun provideForcedChainMixinFactory(
        chainRegistry: ChainRegistry,
        payload: AddAccountPayload,
    ): MixinFactory<ForcedChainMixin> {
        return ForcedChainMixinFactory(chainRegistry, payload)
    }

    @Provides
    fun provideNameChooserMixinFactory(
        payload: AddAccountPayload,
    ): MixinFactory<AccountNameChooserMixin.Presentation> {
        return AccountNameChooserFactory(payload)
    }

    @Provides
    @ScreenScope
    fun provideFileReader(context: Context) = FileReader(context)

    @Provides
    @IntoMap
    @ViewModelKey(ImportAccountViewModel::class)
    fun provideViewModel(
        interactor: AccountInteractor,
        addAccountInteractor: AddAccountInteractor,
        router: AccountRouter,
        resourceManager: ResourceManager,
        forcedChainMixinFactory: MixinFactory<ForcedChainMixin>,
        cryptoChooserMixinFactory: MixinFactory<CryptoTypeChooserMixin>,
        accountNameChooserFactory: MixinFactory<AccountNameChooserMixin.Presentation>,
        clipboardManager: ClipboardManager,
        fileReader: FileReader,
        payload: AddAccountPayload,
    ): ViewModel {
        return ImportAccountViewModel(
            addAccountInteractor,
            interactor,
            router,
            resourceManager,
            forcedChainMixinFactory,
            cryptoChooserMixinFactory,
            accountNameChooserFactory,
            clipboardManager,
            fileReader,
            payload
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
