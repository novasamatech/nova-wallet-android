package io.novafoundation.nova.feature_account_impl.presentation.settings.di

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
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.settings.SettingsViewModel
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor

@Module(includes = [ViewModelModule::class])
class SettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    fun provideViewModel(
        accountInteractor: AccountInteractor,
        router: AccountRouter,
        appLinksProvider: AppLinksProvider,
        resourceManager: ResourceManager,
        appVersionProvider: AppVersionProvider,
        selectedAccountUseCase: SelectedAccountUseCase,
        currencyInteractor: CurrencyInteractor,
        safeModeService: SafeModeService,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        twoFactorVerificationService: TwoFactorVerificationService
    ): ViewModel {
        return SettingsViewModel(
            accountInteractor,
            router,
            appLinksProvider,
            resourceManager,
            appVersionProvider,
            selectedAccountUseCase,
            currencyInteractor,
            safeModeService,
            actionAwaitableMixinFactory,
            twoFactorVerificationService
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): SettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SettingsViewModel::class.java)
    }
}
