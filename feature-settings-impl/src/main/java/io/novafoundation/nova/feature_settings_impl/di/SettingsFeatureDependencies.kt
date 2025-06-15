package io.novafoundation.nova.feature_settings_impl.di

import android.content.Context
import coil.ImageLoader
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.coingecko.CoinGeckoLinkParser
import io.novafoundation.nova.common.data.repository.AssetsIconModeRepository
import io.novafoundation.nova.common.data.repository.BannerVisibilityRepository
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.AppVersionProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.SafeModeService
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import io.novafoundation.nova.common.sequrity.biometry.BiometricServiceFactory
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixinFactory
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.common.view.input.selector.ListSelectorMixin
import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.MetaAccountTypePresentationMapper
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.language.LanguageUseCase
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.WelcomePushNotificationsInteractor
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.runtime.ethereum.Web3ApiFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnectionFactory
import io.novafoundation.nova.runtime.multiNetwork.connection.node.healthState.NodeHealthStateTesterFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProviderPool
import io.novafoundation.nova.runtime.repository.ChainNodeRepository
import io.novafoundation.nova.runtime.repository.ChainRepository
import io.novafoundation.nova.runtime.repository.PreConfiguredChainsRepository
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.CoinGeckoLinkValidationFactory

interface SettingsFeatureDependencies {

    val cloudBackupService: CloudBackupService

    val cloudBackupFacade: LocalAccountsCloudBackupFacade

    val bannerVisRepository: BannerVisibilityRepository

    val runtimeProviderPool: RuntimeProviderPool

    val nodeHealthStateTesterFactory: NodeHealthStateTesterFactory

    val chainNodeRepository: ChainNodeRepository

    val nodeConnectionFactory: NodeConnectionFactory

    val web3ApiFactory: Web3ApiFactory

    val validationExecutor: ValidationExecutor

    val preConfiguredChainsRepository: PreConfiguredChainsRepository

    val coinGeckoLinkParser: CoinGeckoLinkParser

    val chainRepository: ChainRepository

    val coinGeckoLinkValidationFactory: CoinGeckoLinkValidationFactory

    val assetsIconModeRepository: AssetsIconModeRepository

    val accountRepository: AccountRepository

    val accountInteractor: AccountInteractor

    val chainRegistry: ChainRegistry

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

    val imageLoader: ImageLoader

    val metaAccountTypePresentationMapper: MetaAccountTypePresentationMapper

    val addressIconGenerator: AddressIconGenerator

    val walletUiUseCase: WalletUiUseCase

    val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper

    fun biometricServiceFactory(): BiometricServiceFactory

    fun twoFactorVerificationService(): TwoFactorVerificationService

    fun provideListSelectorMixinFactory(): ListSelectorMixin.Factory

    fun actionBottomSheetLauncherFactory(): ActionBottomSheetLauncherFactory

    fun progressDialogMixinFactory(): ProgressDialogMixinFactory

    fun customDialogProvider(): CustomDialogDisplayer.Presentation

    fun context(): Context
}
