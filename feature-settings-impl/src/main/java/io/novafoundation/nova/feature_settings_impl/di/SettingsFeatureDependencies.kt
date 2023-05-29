package io.novafoundation.nova.feature_settings_impl.di

import android.content.Context
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.AppVersionProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.SafeModeService
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import io.novafoundation.nova.common.sequrity.biometry.BiometricServiceFactory
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.language.LanguageUseCase
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase

interface SettingsFeatureDependencies {

    val languageUseCase: LanguageUseCase

    val appLinksProvider: AppLinksProvider

    val resourceManager: ResourceManager

    val appVersionProvider: AppVersionProvider

    val selectedAccountUseCase: SelectedAccountUseCase

    val currencyInteractor: CurrencyInteractor

    val safeModeService: SafeModeService

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val walletConnectSessionsUseCase: WalletConnectSessionsUseCase

    fun biometricServiceFactory(): BiometricServiceFactory

    fun twoFactorVerificationService(): TwoFactorVerificationService

    fun context(): Context
}
