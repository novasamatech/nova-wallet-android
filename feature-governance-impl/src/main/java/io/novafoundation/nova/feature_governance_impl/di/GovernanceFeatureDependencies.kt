package io.novafoundation.nova.feature_governance_impl.di

import android.content.Context
import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.input.chooser.ListChooserMixin
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.dao.GovernanceDAppsDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.LocalIdentity
import io.novafoundation.nova.feature_account_api.domain.account.identity.OnChainIdentity
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.di.ExtrinsicSerialization
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import io.novafoundation.nova.runtime.storage.SampledBlockTimeStorage
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

interface GovernanceFeatureDependencies {

    val feeLoaderMixinFactory: FeeLoaderMixin.Factory

    val validationExecutor: ValidationExecutor

    val preferences: Preferences

    val walletRepository: WalletRepository

    val chainRegistry: ChainRegistry

    val imageLoader: ImageLoader

    val addressIconGenerator: AddressIconGenerator

    val resourceManager: ResourceManager

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val tokenRepository: TokenRepository

    val accountRepository: AccountRepository

    val selectedAccountUseCase: SelectedAccountUseCase

    val chainStateRepository: ChainStateRepository

    val totalIssuanceRepository: TotalIssuanceRepository

    val storageCache: StorageCache

    val sampledBlockTimeStorage: SampledBlockTimeStorage

    val dAppMetadataRepository: DAppMetadataRepository

    val externalAccountActions: ExternalActions.Presentation

    val context: Context

    val amountMixinFactory: AmountChooserMixin.Factory

    val extrinsicService: ExtrinsicService

    val resourceHintsMixinFactory: ResourcesHintsMixinFactory

    val walletUiUseCase: WalletUiUseCase

    val balanceLocksRepository: BalanceLocksRepository

    val computationalCache: ComputationalCache

    val governanceDAppsDao: GovernanceDAppsDao

    val networkApiCreator: NetworkApiCreator

    @ExtrinsicSerialization
    fun extrinsicGson(): Gson

    @LocalIdentity
    fun localIdentityProvider(): IdentityProvider

    @OnChainIdentity
    fun onChainIdentityProvider(): IdentityProvider

    @Named(REMOTE_STORAGE_SOURCE)
    fun remoteStorageDataSource(): StorageDataSource

    val onChainIdentityRepository: OnChainIdentityRepository

    val listChooserMixinFactory: ListChooserMixin.Factory
}
