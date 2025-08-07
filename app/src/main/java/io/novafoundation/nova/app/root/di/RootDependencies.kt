package io.novafoundation.nova.app.root.di

import android.content.Context
import coil.ImageLoader
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.NetworkStateMixin
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.SafeModeService
import io.novafoundation.nova.common.utils.ToastMessageManager
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.core_db.dao.BrowserTabsDao
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.externalAccounts.ExternalAccountsSyncService
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.di.deeplinks.AccountDeepLinks
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.cloudBackup.ApplyLocalSnapshotToCloudBackupUseCase
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_migration.di.deeplinks.AccountMigrationDeepLinks
import io.novafoundation.nova.feature_assets.data.network.BalancesUpdateSystem
import io.novafoundation.nova.feature_assets.di.modules.deeplinks.AssetDeepLinks
import io.novafoundation.nova.feature_buy_api.di.deeplinks.BuyDeepLinks
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_dapp_api.data.repository.BrowserTabExternalRepository
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_api.di.deeplinks.DAppDeepLinks
import io.novafoundation.nova.feature_deep_linking.presentation.handling.PendingDeepLinkProvider
import io.novafoundation.nova.feature_deep_linking.presentation.handling.branchIo.BranchIoLinkConverter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.common.DeepLinkingPreferences
import io.novafoundation.nova.feature_governance_api.data.MutableGovernanceState
import io.novafoundation.nova.feature_governance_api.di.deeplinks.GovernanceDeepLinks
import io.novafoundation.nova.feature_multisig_operations.di.deeplink.MultisigDeepLinks
import io.novafoundation.nova.feature_push_notifications.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.WelcomePushNotificationsInteractor
import io.novafoundation.nova.feature_staking_api.di.deeplinks.StakingDeepLinks
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.MultisigExtrinsicValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.ProxyHaveEnoughFeeValidationFactory
import io.novafoundation.nova.feature_wallet_connect_api.di.deeplinks.WalletConnectDeepLinks
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import kotlinx.coroutines.flow.MutableStateFlow

interface RootDependencies {

    val stakingDeepLinks: StakingDeepLinks

    val accountDeepLinks: AccountDeepLinks

    val dAppDeepLinks: DAppDeepLinks

    val governanceDeepLinks: GovernanceDeepLinks

    val buyDeepLinks: BuyDeepLinks

    val assetDeepLinks: AssetDeepLinks

    val walletConnectDeepLinks: WalletConnectDeepLinks

    val systemCallExecutor: SystemCallExecutor

    val contextManager: ContextManager

    val walletConnectService: WalletConnectService

    val imageLoader: ImageLoader

    val automaticInteractionGate: AutomaticInteractionGate

    val walletConnectSessionsUseCase: WalletConnectSessionsUseCase

    val pushNotificationsInteractor: PushNotificationsInteractor

    val welcomePushNotificationsInteractor: WelcomePushNotificationsInteractor

    val applyLocalSnapshotToCloudBackupUseCase: ApplyLocalSnapshotToCloudBackupUseCase

    val actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory

    val tabsDao: BrowserTabsDao

    val balancesUpdateSystem: BalancesUpdateSystem

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val browserTabExternalRepository: BrowserTabExternalRepository

    val externalAccountsSyncService: ExternalAccountsSyncService

    val multisigPendingOperationsService: MultisigPendingOperationsService

    val accountMigrationDeepLinks: AccountMigrationDeepLinks

    val multisigDeepLinks: MultisigDeepLinks

    val deepLinkingPreferences: DeepLinkingPreferences

    val branchIoLinkConverter: BranchIoLinkConverter

    val pendingDeepLinkProvider: PendingDeepLinkProvider

    val multisigExtrinsicValidationRequestBus: MultisigExtrinsicValidationRequestBus

    val multisigExtrinsicValidationFactory: MultisigExtrinsicValidationFactory

    val actionBottomSheetLauncher: ActionBottomSheetLauncher

    fun updateNotificationsInteractor(): UpdateNotificationsInteractor

    fun contributionsInteractor(): ContributionsInteractor

    fun crowdloanRepository(): CrowdloanRepository

    fun networkStateMixin(): NetworkStateMixin

    fun externalRequirementsFlow(): MutableStateFlow<ChainConnection.ExternalRequirement>

    fun accountRepository(): AccountRepository

    fun walletRepository(): WalletRepository

    fun appLinksProvider(): AppLinksProvider

    fun resourceManager(): ResourceManager

    fun currencyInteractor(): CurrencyInteractor

    fun stakingRepository(): StakingRepository

    fun chainRegistry(): ChainRegistry

    fun backgroundAccessObserver(): BackgroundAccessObserver

    fun safeModeService(): SafeModeService

    fun rootScope(): RootScope

    fun governanceStateUpdater(): MutableGovernanceState

    fun dappMetadataRepository(): DAppMetadataRepository

    fun encryptionDefaults(): EncryptionDefaults

    fun proxyExtrinsicValidationRequestBus(): ProxyExtrinsicValidationRequestBus

    fun metaAccountChangesRequestBus(): MetaAccountChangesEventBus

    fun proxyHaveEnoughFeeValidationFactory(): ProxyHaveEnoughFeeValidationFactory

    fun context(): Context

    fun toastMessageManager(): ToastMessageManager
}
