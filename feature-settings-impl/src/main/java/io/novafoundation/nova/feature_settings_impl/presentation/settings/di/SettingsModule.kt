package io.novafoundation.nova.feature_settings_impl.presentation.settings.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.AppVersionProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.SafeModeService
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.language.LanguageUseCase
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.presentation.settings.SettingsViewModel
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase

@Module(includes = [ViewModelModule::class])
class SettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    fun provideViewModel(
        languageUseCase: LanguageUseCase,
        router: SettingsRouter,
        appLinksProvider: AppLinksProvider,
        resourceManager: ResourceManager,
        appVersionProvider: AppVersionProvider,
        selectedAccountUseCase: SelectedAccountUseCase,
        currencyInteractor: CurrencyInteractor,
        safeModeService: SafeModeService,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        walletConnectSessionsUseCase: WalletConnectSessionsUseCase,
    ): ViewModel {
        return SettingsViewModel(
            languageUseCase,
            router,
            appLinksProvider,
            resourceManager,
            appVersionProvider,
            selectedAccountUseCase,
            currencyInteractor,
            safeModeService,
            actionAwaitableMixinFactory,
            walletConnectSessionsUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): SettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SettingsViewModel::class.java)
    }
}
