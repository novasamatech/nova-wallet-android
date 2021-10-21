package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup.di

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
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.CryptoTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl.CryptoTypeChooserFactory
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup.BackupMnemonicViewModel

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
