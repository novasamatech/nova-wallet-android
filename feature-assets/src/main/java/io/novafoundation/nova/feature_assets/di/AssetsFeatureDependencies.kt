package io.novafoundation.nova.feature_assets.di

import android.content.ContentResolver
import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.format.EthereumAddressFormat
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.coingecko.CoingeckoApi
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionHistoryRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.qr.MultiChainQrSharingFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.web3names.domain.networking.Web3NamesInteractor
import jp.co.soramitsu.fearless_utils.encrypt.Signer
import jp.co.soramitsu.fearless_utils.icon.IconGenerator
import jp.co.soramitsu.fearless_utils.wsrpc.logging.Logger
import javax.inject.Named

interface AssetsFeatureDependencies {

    fun web3NamesInteractor(): Web3NamesInteractor

    fun contributionsInteractor(): ContributionsInteractor

    fun contributionsRepository(): ContributionsRepository

    fun locksDao(): LockDao

    fun currencyInteractor(): CurrencyInteractor

    fun currencyRepository(): CurrencyRepository

    fun metaAccountGroupingInteractor(): MetaAccountGroupingInteractor

    fun accountInteractor(): AccountInteractor

    fun preferences(): Preferences

    fun encryptedPreferences(): EncryptedPreferences

    fun resourceManager(): ResourceManager

    fun iconGenerator(): IconGenerator

    fun clipboardManager(): ClipboardManager

    fun contentResolver(): ContentResolver

    fun accountRepository(): AccountRepository

    fun networkCreator(): NetworkApiCreator

    fun signer(): Signer

    fun logger(): Logger

    fun jsonMapper(): Gson

    fun addressIconGenerator(): AddressIconGenerator

    fun appLinksProvider(): AppLinksProvider

    fun qrCodeGenerator(): QrCodeGenerator

    fun fileProvider(): FileProvider

    fun externalAccountActions(): ExternalActions.Presentation

    fun httpExceptionHandler(): HttpExceptionHandler

    fun addressDisplayUseCase(): AddressDisplayUseCase

    fun chainRegistry(): ChainRegistry

    @Named(REMOTE_STORAGE_SOURCE)
    fun remoteStorageSource(): StorageDataSource

    @Named(LOCAL_STORAGE_SOURCE)
    fun localStorageSource(): StorageDataSource

    fun extrinsicService(): ExtrinsicService

    fun imageLoader(): ImageLoader

    fun selectedAccountUseCase(): SelectedAccountUseCase

    fun validationExecutor(): ValidationExecutor

    fun eventsRepository(): EventsRepository

    fun walletRepository(): WalletRepository

    fun feeLoaderMixinFactory(): FeeLoaderMixin.Factory

    fun amountChooserFactory(): AmountChooserMixin.Factory

    fun walletConstants(): WalletConstants

    fun ethereumAddressFormat(): EthereumAddressFormat

    fun coingeckoApi(): CoingeckoApi

    fun walletConnectSessionsUseCase(): WalletConnectSessionsUseCase

    val assetsSourceRegistry: AssetSourceRegistry

    fun nftRepository(): NftRepository

    val addressInputMixinFactory: AddressInputMixinFactory

    val multiChainQrSharingFactory: MultiChainQrSharingFactory

    val walletUiUseCase: WalletUiUseCase

    val computationalCache: ComputationalCache

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val crossChainTraRepository: CrossChainTransfersRepository
    val crossChainWeigher: CrossChainWeigher
    val crossChainTransactor: CrossChainTransactor

    val resourcesHintsMixinFactory: ResourcesHintsMixinFactory

    val parachainInfoRepository: ParachainInfoRepository

    val watchOnlyMissingKeysPresenter: WatchOnlyMissingKeysPresenter

    val balanceLocksRepository: BalanceLocksRepository

    val transactionHistoryRepository: TransactionHistoryRepository

    val chainAssetRepository: ChainAssetRepository

    val erc20Standard: Erc20Standard
}
