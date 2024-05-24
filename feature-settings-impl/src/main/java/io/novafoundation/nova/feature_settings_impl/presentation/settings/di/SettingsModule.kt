package io.novafoundation.nova.feature_settings_impl.presentation.settings.di

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.io.MainThreadExecutor
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.AppVersionProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.SafeModeService
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import io.novafoundation.nova.common.sequrity.biometry.BiometricPromptFactory
import io.novafoundation.nova.common.sequrity.biometry.BiometricService
import io.novafoundation.nova.common.sequrity.biometry.BiometricServiceFactory
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.language.LanguageUseCase
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_settings_impl.R
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
        twoFactorVerificationService: TwoFactorVerificationService,
        biometricService: BiometricService,
        pushNotificationsInteractor: PushNotificationsInteractor
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
            walletConnectSessionsUseCase,
            twoFactorVerificationService,
            biometricService,
            pushNotificationsInteractor
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): SettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SettingsViewModel::class.java)
    }

    @Provides
    fun provideBiometricService(
        fragment: Fragment,
        context: Context,
        resourceManager: ResourceManager,
        biometricServiceFactory: BiometricServiceFactory
    ): BiometricService {
        val biometricManager = BiometricManager.from(context)
        val biometricPromptFactory = BiometricPromptFactory(fragment, MainThreadExecutor())
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(resourceManager.getString(R.string.biometric_auth_title))
            .setSubtitle(resourceManager.getString(R.string.pincode_biometry_dialog_subtitle))
            .setNegativeButtonText(resourceManager.getString(R.string.common_cancel))
            .build()

        return biometricServiceFactory.create(biometricManager, biometricPromptFactory, promptInfo)
    }
}
