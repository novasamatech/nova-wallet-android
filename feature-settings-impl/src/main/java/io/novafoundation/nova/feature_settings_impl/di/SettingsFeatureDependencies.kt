package io.novafoundation.nova.feature_settings_impl.di

import android.content.Context
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.AppVersionProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.SafeModeService
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import io.novafoundation.nova.common.sequrity.biometry.BiometricServiceFactory
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixin
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.input.selector.ListSelectorMixin
import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.language.LanguageUseCase
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.WelcomePushNotificationsInteractor
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

    val pushNotificationsInteractor: PushNotificationsInteractor

    val welcomePushNotificationsInteractor: WelcomePushNotificationsInteractor

    fun biometricServiceFactory(): BiometricServiceFactory

    fun twoFactorVerificationService(): TwoFactorVerificationService

    fun provideListSelectorMixinFactory(): ListSelectorMixin.Factory

    fun actionBottomSheetLauncher(): ActionBottomSheetLauncher

    fun progressDialogMixin(): ProgressDialogMixin

    fun context(): Context

    val cloudBackupService: CloudBackupService

    val cloudBackupFacade: LocalAccountsCloudBackupFacade
}
