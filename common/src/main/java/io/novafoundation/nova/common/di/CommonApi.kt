package io.novafoundation.nova.common.di

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.format.AddressSchemeFormatter
import io.novafoundation.nova.common.address.format.EthereumAddressFormat
import io.novafoundation.nova.common.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.network.coingecko.CoinGeckoLinkParser
import io.novafoundation.nova.common.data.network.rpc.SocketSingleRequestExecutor
import io.novafoundation.nova.common.data.repository.AssetsIconModeRepository
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.common.data.repository.BannerVisibilityRepository
import io.novafoundation.nova.common.data.repository.ToggleFeatureRepository
import io.novafoundation.nova.common.data.secrets.v1.SecretStoreV1
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.di.modules.Caching
import io.novafoundation.nova.common.domain.interactor.AssetViewModeInteractor
import io.novafoundation.nova.common.interfaces.ActivityIntentProvider
import io.novafoundation.nova.common.interfaces.BuildTypeProvider
import io.novafoundation.nova.common.interfaces.FileCache
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.NetworkStateMixin
import io.novafoundation.nova.common.mixin.condition.ConditionMixinFactory
import io.novafoundation.nova.common.mixin.copy.CopyTextLauncher
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.AppVersionProvider
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.LanguagesHolder
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.SafeModeService
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationExecutor
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import io.novafoundation.nova.common.utils.CopyValueMixin
import io.novafoundation.nova.common.utils.IntegrityService
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.utils.ToastMessageManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.browser.fileChoosing.WebViewFileChooserFactory
import io.novafoundation.nova.common.utils.browser.permissions.WebViewPermissionAskerFactory
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.ip.IpAddressReceiver
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.multiResult.PartialRetriableMixin
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixinFactory
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver
import io.novafoundation.nova.common.utils.splash.SplashPassedObserver
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.common.utils.webView.InterceptingWebViewClientFactory
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.vibration.DeviceVibrator
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.common.view.input.chooser.ListChooserMixin
import io.novafoundation.nova.common.view.input.selector.ListSelectorMixin
import io.novafoundation.nova.common.view.parallaxCard.BackingParallaxCardLruCache
import io.novasama.substrate_sdk_android.encrypt.Signer
import io.novasama.substrate_sdk_android.icon.IconGenerator
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import okhttp3.OkHttpClient
import java.util.Random

interface CommonApi {

    val systemCallExecutor: SystemCallExecutor

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val resourcesHintsMixinFactory: ResourcesHintsMixinFactory

    val okHttpClient: OkHttpClient

    val fileCache: FileCache

    val permissionsAskerFactory: PermissionsAskerFactory

    val bluetoothManager: BluetoothManager

    val locationManager: LocationManager

    val listChooserMixinFactory: ListChooserMixin.Factory

    val partialRetriableMixinFactory: PartialRetriableMixin.Factory

    val automaticInteractionGate: AutomaticInteractionGate

    val bannerVisibilityRepository: BannerVisibilityRepository

    val provideActivityIntentProvider: ActivityIntentProvider

    val googleApiAvailabilityProvider: GoogleApiAvailabilityProvider

    val coinGeckoLinkParser: CoinGeckoLinkParser

    val webViewPermissionAskerFactory: WebViewPermissionAskerFactory

    val webViewFileChooserFactory: WebViewFileChooserFactory

    val interceptingWebViewClientFactory: InterceptingWebViewClientFactory

    val addressSchemeFormatter: AddressSchemeFormatter

    val splashPassedObserver: SplashPassedObserver

    val toggleFeatureRepository: ToggleFeatureRepository

    val ipAddressReceiver: IpAddressReceiver

    val actionBottomSheetLauncher: ActionBottomSheetLauncher

    val integrityService: IntegrityService

    fun copyTextMixin(): CopyTextLauncher.Presentation

    fun computationalCache(): ComputationalCache

    fun imageLoader(): ImageLoader

    fun context(): Context

    fun provideResourceManager(): ResourceManager

    fun provideNetworkApiCreator(): NetworkApiCreator

    fun provideAppLinksProvider(): AppLinksProvider

    fun providePreferences(): Preferences

    fun backgroundAccessObserver(): BackgroundAccessObserver

    fun provideEncryptedPreferences(): EncryptedPreferences

    fun provideIconGenerator(): IconGenerator

    fun provideClipboardManager(): ClipboardManager

    fun provideDeviceVibrator(): DeviceVibrator

    fun signer(): Signer

    fun logger(): Logger

    fun contextManager(): ContextManager

    fun languagesHolder(): LanguagesHolder

    fun provideJsonMapper(): Gson

    fun socketServiceCreator(): SocketService

    fun provideSocketSingleRequestExecutor(): SocketSingleRequestExecutor

    fun addressIconGenerator(): AddressIconGenerator

    @Caching
    fun cachingAddressIconGenerator(): AddressIconGenerator

    fun networkStateMixin(): NetworkStateMixin

    fun qrCodeGenerator(): QrCodeGenerator

    fun fileProvider(): FileProvider

    fun random(): Random

    fun contentResolver(): ContentResolver

    fun httpExceptionHandler(): HttpExceptionHandler

    fun validationExecutor(): ValidationExecutor

    fun secretStoreV1(): SecretStoreV1

    fun secretStoreV2(): SecretStoreV2

    fun customDialogDisplayer(): CustomDialogDisplayer.Presentation

    fun appVersionsProvider(): AppVersionProvider

    fun ethereumAddressFormat(): EthereumAddressFormat

    fun sharedPreferences(): SharedPreferences

    fun safeModeService(): SafeModeService

    fun twoFactorVerificationService(): TwoFactorVerificationService

    fun twoFactorVerificationExecutor(): TwoFactorVerificationExecutor

    fun rootScope(): RootScope

    fun bakingParallaxCardCache(): BackingParallaxCardLruCache

    fun descriptionBottomSheetLauncher(): DescriptionBottomSheetLauncher

    fun provideActionBottomSheetLauncherFactory(): ActionBottomSheetLauncherFactory

    fun progressDialogMixinFactory(): ProgressDialogMixinFactory

    fun provideListSelectorMixinFactory(): ListSelectorMixin.Factory

    fun provideConditionMixinFactory(): ConditionMixinFactory

    fun buildTypeProvider(): BuildTypeProvider

    fun assetsViewModeRepository(): AssetsViewModeRepository

    fun assetsIconModeService(): AssetsIconModeRepository

    fun assetIconProvider(): AssetIconProvider

    fun assetViewModeInteractor(): AssetViewModeInteractor

    fun toastMessageManager(): ToastMessageManager

    fun copyValueMixin(): CopyValueMixin
}
