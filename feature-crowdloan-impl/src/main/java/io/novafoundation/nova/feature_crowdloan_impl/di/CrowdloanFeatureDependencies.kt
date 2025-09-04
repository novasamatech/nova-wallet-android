package io.novafoundation.nova.feature_crowdloan_impl.di

import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.dao.ContributionDao
import io.novafoundation.nova.core_db.dao.ExternalBalanceDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

interface CrowdloanFeatureDependencies {

    val parachainInfoRepository: ParachainInfoRepository

    val signerProvider: SignerProvider

    val storageStorageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory

    val externalBalanceDao: ExternalBalanceDao

    val assetIconProvider: AssetIconProvider

    fun contributionDao(): ContributionDao

    fun accountUpdaterScope(): AccountUpdateScope

    fun selectedAccountUseCase(): SelectedAccountUseCase

    fun walletConstants(): WalletConstants

    fun storageCache(): StorageCache

    fun imageLoader(): ImageLoader

    fun accountRepository(): AccountRepository

    fun addressIconGenerator(): AddressIconGenerator

    fun appLinksProvider(): AppLinksProvider

    fun walletRepository(): WalletRepository

    fun tokenRepository(): TokenRepository

    fun resourceManager(): ResourceManager

    fun externalAccountActions(): ExternalActions.Presentation

    fun networkApiCreator(): NetworkApiCreator

    fun httpExceptionHandler(): HttpExceptionHandler

    fun gson(): Gson

    fun addressxDisplayUseCase(): AddressDisplayUseCase

    fun extrinsicService(): ExtrinsicService

    fun validationExecutor(): ValidationExecutor

    @Named(REMOTE_STORAGE_SOURCE)
    fun remoteStorageSource(): StorageDataSource

    @Named(LOCAL_STORAGE_SOURCE)
    fun localStorageSource(): StorageDataSource

    fun chainStateRepository(): ChainStateRepository

    fun chainRegistry(): ChainRegistry

    fun preferences(): Preferences

    fun secretStoreV2(): SecretStoreV2

    fun customDialogDisplayer(): CustomDialogDisplayer.Presentation

    fun feeLoaderMixinFactory(): FeeLoaderMixin.Factory
}
