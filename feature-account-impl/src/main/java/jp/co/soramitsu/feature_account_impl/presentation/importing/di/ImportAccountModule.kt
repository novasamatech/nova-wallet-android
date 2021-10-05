package jp.co.soramitsu.feature_account_impl.presentation.importing.di

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import jp.co.soramitsu.common.di.scope.ScreenScope
import jp.co.soramitsu.common.di.viewmodel.ViewModelKey
import jp.co.soramitsu.common.di.viewmodel.ViewModelModule
import jp.co.soramitsu.common.mixin.MixinFactory
import jp.co.soramitsu.common.resources.ClipboardManager
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.core.model.Node
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountInteractor
import jp.co.soramitsu.feature_account_api.presenatation.account.add.AddAccountPayload
import jp.co.soramitsu.feature_account_impl.domain.account.add.AddAccountInteractor
import jp.co.soramitsu.feature_account_impl.presentation.AccountRouter
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.CryptoTypeChooserMixin
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.ForcedChainMixin
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.impl.ForcedChainMixinFactory
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.impl.ForcedChainProvider
import jp.co.soramitsu.feature_account_impl.presentation.importing.FileReader
import jp.co.soramitsu.feature_account_impl.presentation.importing.ImportAccountViewModel
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ImportAccountModule {

    @Provides
    @ScreenScope
    fun provideForcedChainMixinFactory(
        chainRegistry: ChainRegistry,
        payload: AddAccountPayload
    ): MixinFactory<ForcedChainMixin> {
        return ForcedChainMixinFactory(chainRegistry, payload)
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
        cryptoChooserMixin: CryptoTypeChooserMixin,
        clipboardManager: ClipboardManager,
        fileReader: FileReader,
        payload: AddAccountPayload
    ): ViewModel {
        return ImportAccountViewModel(
            addAccountInteractor,
            interactor,
            router,
            resourceManager,
            cryptoChooserMixin,
            forcedChainMixinFactory,
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
