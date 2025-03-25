package io.novafoundation.nova.feature_account_impl.di

import android.content.Context
import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.rpc.SocketSingleRequestExecutor
import io.novafoundation.nova.common.data.secrets.v1.SecretStoreV1
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.di.modules.Caching
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.condition.ConditionMixinFactory
import io.novafoundation.nova.common.resources.AppVersionProvider
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.LanguagesHolder
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.SafeModeService
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationExecutor
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import io.novafoundation.nova.common.utils.CopyValueMixin
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.vibration.DeviceVibrator
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.common.view.input.selector.ListSelectorMixin
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.dao.AccountDao
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.dao.NodeDao
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixinFactory
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathQuoter
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuoting
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ethereum.gas.GasPriceProviderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.extrinsic.MortalityConstructor
import io.novafoundation.nova.runtime.extrinsic.multi.ExtrinsicSplitter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.XcmVersionDetector
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.multiNetwork.qr.MultiChainQrSharingFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.web3names.domain.networking.Web3NamesInteractor
import io.novasama.substrate_sdk_android.icon.IconGenerator
import java.util.Random
import javax.inject.Named

interface AccountFeatureDependencies {

    val hydraDxAssetConversionFactory: HydraDxQuoting.Factory

    val pathQuoterFactory: PathQuoter.Factory

    val hydraDxAssetIdConverter: HydraDxAssetIdConverter

    val systemCallExecutor: SystemCallExecutor

    val multiChainQrSharingFactory: MultiChainQrSharingFactory

    val contextManager: ContextManager

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val permissionsAskerFactory: PermissionsAskerFactory

    val qrCodeGenerator: QrCodeGenerator

    val mortalityConstructor: MortalityConstructor

    val currencyRepository: CurrencyRepository

    val extrinsicValidityUseCase: ExtrinsicValidityUseCase

    val extrinsicSplitter: ExtrinsicSplitter

    val gasPriceProviderFactory: GasPriceProviderFactory

    val rootScope: RootScope

    val cloudBackupChangingWarningMixinFactory: CloudBackupChangingWarningMixinFactory

    val listSelectorMixinFactory: ListSelectorMixin.Factory

    val ledgerMigrationTracker: LedgerMigrationTracker

    val multiLocationConverterFactory: MultiLocationConverterFactory

    val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory

    val storageCache: StorageCache

    val eventsRepository: EventsRepository

    val xcmVersionDetector: XcmVersionDetector

    val chainStateRepository: ChainStateRepository

    fun appLinksProvider(): AppLinksProvider

    fun preferences(): Preferences

    fun encryptedPreferences(): EncryptedPreferences

    fun resourceManager(): ResourceManager

    fun iconGenerator(): IconGenerator

    fun clipboardManager(): ClipboardManager

    fun context(): Context

    fun deviceVibrator(): DeviceVibrator

    fun userDao(): AccountDao

    fun nodeDao(): NodeDao

    fun languagesHolder(): LanguagesHolder

    fun socketSingleRequestExecutor(): SocketSingleRequestExecutor

    fun jsonMapper(): Gson

    fun addressIconGenerator(): AddressIconGenerator

    fun currencyInteractor(): CurrencyInteractor

    @Caching
    fun cachingIconGenerator(): AddressIconGenerator

    fun random(): Random

    fun secretStoreV1(): SecretStoreV1

    fun secretStoreV2(): SecretStoreV2

    fun metaAccountDao(): MetaAccountDao

    fun chainRegistry(): ChainRegistry

    fun extrinsicBuilderFactory(): ExtrinsicBuilderFactory

    fun rpcCalls(): RpcCalls

    fun imageLoader(): ImageLoader

    fun backgroundAccessObserver(): BackgroundAccessObserver

    fun appVersionProvider(): AppVersionProvider

    fun validationExecutor(): ValidationExecutor

    fun updateNotificationsInteractor(): UpdateNotificationsInteractor

    fun safeModeService(): SafeModeService

    fun web3NamesInteractor(): Web3NamesInteractor

    fun twoFactorVerificationService(): TwoFactorVerificationService

    fun twoFactorVerificationExecutor(): TwoFactorVerificationExecutor

    fun computationalCache(): ComputationalCache

    fun getProxyRepository(): GetProxyRepository

    fun cloudBackupService(): CloudBackupService

    fun provideActionBottomSheetLauncherFactory(): ActionBottomSheetLauncherFactory

    fun customDialogProvider(): CustomDialogDisplayer.Presentation

    fun provideConditionMixinFactory(): ConditionMixinFactory

    fun multiChainRuntimeCallsApi(): MultiChainRuntimeCallsApi

    fun copyValueMixin(): CopyValueMixin

    @Named(REMOTE_STORAGE_SOURCE)
    fun remoteStorageSource(): StorageDataSource
}
