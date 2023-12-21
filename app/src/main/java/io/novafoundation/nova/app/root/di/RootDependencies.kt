package io.novafoundation.nova.app.root.di

import coil.ImageLoader
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.NetworkStateMixin
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.SafeModeService
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_assets.data.network.BalancesUpdateSystem
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_governance_api.data.MutableGovernanceState
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.ProxyHaveEnoughFeeValidationFactory
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import kotlinx.coroutines.flow.MutableStateFlow

interface RootDependencies {

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

    val balancesUpdateSystem: BalancesUpdateSystem

    fun stakingRepository(): StakingRepository

    fun chainRegistry(): ChainRegistry

    fun backgroundAccessObserver(): BackgroundAccessObserver

    fun safeModeService(): SafeModeService

    fun rootScope(): RootScope

    fun proxySyncService(): ProxySyncService

    fun governanceStateUpdater(): MutableGovernanceState

    fun dappMetadataRepository(): DAppMetadataRepository

    fun encryptionDefaults(): EncryptionDefaults

    fun proxyExtrinsicValidationRequestBus(): ProxyExtrinsicValidationRequestBus

    fun proxyHaveEnoughFeeValidationFactory(): ProxyHaveEnoughFeeValidationFactory

    val systemCallExecutor: SystemCallExecutor

    val contextManager: ContextManager

    val walletConnectService: WalletConnectService

    val imageLoader: ImageLoader

    val automaticInteractionGate: AutomaticInteractionGate

    val walletConnectSessionsUseCase: WalletConnectSessionsUseCase
}
