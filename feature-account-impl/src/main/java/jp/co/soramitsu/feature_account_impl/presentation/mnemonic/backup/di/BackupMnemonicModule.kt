package jp.co.soramitsu.feature_account_impl.presentation.mnemonic.backup.di

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
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.core.model.Node
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountInteractor
import jp.co.soramitsu.feature_account_api.presenatation.account.add.AddAccountPayload
import jp.co.soramitsu.feature_account_impl.presentation.AccountRouter
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.CryptoTypeChooserMixin
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.impl.CryptoTypeChooser
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.impl.CryptoTypeChooserFactory
import jp.co.soramitsu.feature_account_impl.presentation.mnemonic.backup.BackupMnemonicViewModel

@Module(includes = [ViewModelModule::class])
class BackupMnemonicModule {

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
    @IntoMap
    @ViewModelKey(BackupMnemonicViewModel::class)
    fun provideViewModel(
        interactor: AccountInteractor,
        router: AccountRouter,
        accountName: String,
        addAccountPayload: AddAccountPayload,
        cryptoTypeChooserMixinFactory: MixinFactory<CryptoTypeChooserMixin>
    ): ViewModel {
        return BackupMnemonicViewModel(
            interactor,
            router,
            accountName,
            addAccountPayload,
            cryptoTypeChooserMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): BackupMnemonicViewModel {
        return ViewModelProvider(
            fragment,
            viewModelFactory
        ).get(BackupMnemonicViewModel::class.java)
    }
}
