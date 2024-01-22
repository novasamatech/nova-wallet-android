package io.novafoundation.nova.feature_staking_impl.di

import android.content.SharedPreferences
import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.modules.Caching
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.multiResult.PartialRetriableMixin
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.dao.AccountStakingDao
import io.novafoundation.nova.core_db.dao.ExternalBalanceDao
import io.novafoundation.nova.core_db.dao.StakingDashboardDao
import io.novafoundation.nova.core_db.dao.StakingRewardPeriodDao
import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.LocalIdentity
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.IdentityMixin
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_proxy_api.data.common.ProxyDepositCalculator
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.TimestampRepository
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import io.novafoundation.nova.runtime.storage.SampledBlockTimeStorage
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

interface StakingFeatureDependencies {

    fun contextManager(): ContextManager

    fun computationalCache(): ComputationalCache

    fun accountRepository(): AccountRepository

    fun storageCache(): StorageCache

    fun addressIconGenerator(): AddressIconGenerator

    fun appLinksProvider(): AppLinksProvider

    fun walletRepository(): WalletRepository

    fun tokenRepository(): TokenRepository

    fun resourceManager(): ResourceManager

    fun extrinsicBuilderFactory(): ExtrinsicBuilderFactory

    fun substrateCalls(): RpcCalls

    fun externalAccountActions(): ExternalActions.Presentation

    fun assetCache(): AssetCache

    fun accountStakingDao(): AccountStakingDao

    fun accountUpdateScope(): AccountUpdateScope

    fun stakingTotalRewardsDao(): StakingTotalRewardDao

    fun networkApiCreator(): NetworkApiCreator

    fun httpExceptionHandler(): HttpExceptionHandler

    fun walletConstants(): WalletConstants

    fun gson(): Gson

    fun addressxDisplayUseCase(): AddressDisplayUseCase

    fun extrinsicService(): ExtrinsicService

    fun validationExecutor(): ValidationExecutor

    @Named(REMOTE_STORAGE_SOURCE)
    fun remoteStorageSource(): StorageDataSource

    @Named(LOCAL_STORAGE_SOURCE)
    fun localStorageSource(): StorageDataSource

    @LocalIdentity
    fun localIdentity(): IdentityProvider

    fun chainRegistry(): ChainRegistry

    fun imageLoader(): ImageLoader

    fun preferences(): Preferences

    fun feeLoaderMixinFactory(): FeeLoaderMixin.Factory

    fun sharedPreferences(): SharedPreferences

    fun stakingRewardPeriodDao(): StakingRewardPeriodDao

    fun enoughTotalToStayAboveEDValidationFactory(): EnoughTotalToStayAboveEDValidationFactory

    fun addressInputMixinFactory(): AddressInputMixinFactory

    val amountChooserMixinFactory: AmountChooserMixin.Factory

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    @Caching
    fun cachingIconGenerator(): AddressIconGenerator

    val walletUiUseCase: WalletUiUseCase

    val resourcesHintsMixinFactory: ResourcesHintsMixinFactory

    val selectedAccountUseCase: SelectedAccountUseCase

    val chainStateRepository: ChainStateRepository

    val sampledBlockTimeStorage: SampledBlockTimeStorage

    val timestampRepository: TimestampRepository

    val totalIssuanceRepository: TotalIssuanceRepository

    val onChainIdentityRepository: OnChainIdentityRepository

    val identityMixinFactory: IdentityMixin.Factory

    val storageStorageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory

    val stakingDashboardDao: StakingDashboardDao

    val dAppMetadataRepository: DAppMetadataRepository

    val runtimeCallsApi: MultiChainRuntimeCallsApi

    val arbitraryAssetUseCase: ArbitraryAssetUseCase

    val locksRepository: BalanceLocksRepository

    val externalBalanceDao: ExternalBalanceDao

    val partialRetriableMixinFactory: PartialRetriableMixin.Factory

    val proxyDepositCalculator: ProxyDepositCalculator

    val getProxyRepository: GetProxyRepository

    val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher

    val metaAccountGroupingInteractor: MetaAccountGroupingInteractor
}
