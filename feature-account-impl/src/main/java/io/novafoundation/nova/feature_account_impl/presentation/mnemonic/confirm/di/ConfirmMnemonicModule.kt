package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.vibration.DeviceVibrator
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.BuildConfig
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicConfig
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.ConfirmMnemonicPayload
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicViewModel

@Module(includes = [ViewModelModule::class])
class ConfirmMnemonicModule {

    @Provides
    @ScreenScope
    fun provideConfig() = ConfirmMnemonicConfig(
        allowShowingSkip = BuildConfig.DEBUG
    )

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmMnemonicViewModel::class)
    fun provideViewModel(
        interactor: AccountInteractor,
        addAccountInteractor: AddAccountInteractor,
        router: AccountRouter,
        deviceVibrator: DeviceVibrator,
        resourceManager: ResourceManager,
        config: ConfirmMnemonicConfig,
        payload: ConfirmMnemonicPayload
    ): ViewModel {
        return ConfirmMnemonicViewModel(interactor, addAccountInteractor, router, deviceVibrator, resourceManager, config, payload)
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ConfirmMnemonicViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmMnemonicViewModel::class.java)
    }
}
