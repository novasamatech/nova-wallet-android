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
import io.novafoundation.nova.common.data.network.coingecko.CoinGeckoLinkParser
import io.novafoundation.nova.common.data.repository.AssetsIconModeRepository
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.common.data.repository.BannerVisibilityRepository
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.domain.interactor.AssetViewModeInteractor
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.utils.browser.fileChoosing.WebViewFileChooserFactory
import io.novafoundation.nova.common.utils.browser.permissions.WebViewPermissionAskerFactory
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.common.utils.webView.InterceptingWebViewClientFactory
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.core_db.dao.HoldsDao
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.copyAddress.CopyAddressMixin
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressMixin
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixinUi
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoSellRequestInterceptorFactory
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_staking_api.data.mythos.MythosMainPotMatcherFactory
import io.novafoundation.nova.feature_staking_api.data.network.blockhain.updaters.PooledBalanceUpdaterFactory
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.presentation.nominationPools.display.PoolDisplayUseCase
import io.novafoundation.nova.feature_swap_api.domain.interactor.SwapAvailabilityInteractor
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_api.presentation.formatters.SwapRateFormatter
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapFlowScopeAggregator
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.BalanceLocksUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.PaymentUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.priceApi.ProxyPriceApi
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.data.network.priceApi.CoingeckoApi
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceHoldsRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.ExternalBalanceRepository
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.qr.MultiChainQrSharingFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.web3names.domain.networking.Web3NamesInteractor
import io.novasama.substrate_sdk_android.encrypt.Signer
import io.novasama.substrate_sdk_android.icon.IconGenerator
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import okhttp3.OkHttpClient
import javax.inject.Named

interface AssetsFeatureDependencies {

    val feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory

    val assetsSourceRegistry: AssetSourceRegistry

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

    val chainAssetRepository: ChainAssetRepository

    val erc20Standard: Erc20Standard

    val externalBalanceRepository: ExternalBalanceRepository

    val pooledBalanceUpdaterFactory: PooledBalanceUpdaterFactory

    val paymentUpdaterFactory: PaymentUpdaterFactory

    val locksUpdaterFactory: BalanceLocksUpdaterFactory

    val accountUpdateScope: AccountUpdateScope

    val storageSharedRequestBuilderFactory: StorageSharedRequestsBuilderFactory

    val poolDisplayUseCase: PoolDisplayUseCase

    val poolAccountDerivation: PoolAccountDerivation

    val operationDao: OperationDao

    val coinPriceRepository: CoinPriceRepository

    val swapSettingsStateProvider: SwapSettingsStateProvider

    val swapService: SwapService

    val swapAvailabilityInteractor: SwapAvailabilityInteractor

    val bannerVisibilityRepository: BannerVisibilityRepository

    val tradeMixinFactory: TradeMixin.Factory

    val buyMixinUi: BuyMixinUi

    val crossChainTransfersUseCase: CrossChainTransfersUseCase

    val arbitraryTokenUseCase: ArbitraryTokenUseCase

    val swapRateFormatter: SwapRateFormatter

    val bottomSheetLauncher: DescriptionBottomSheetLauncher

    val selectAddressMixinFactory: SelectAddressMixin.Factory

    val chainStateRepository: ChainStateRepository

    val holdsRepository: BalanceHoldsRepository

    val holdsDao: HoldsDao

    val coinGeckoLinkParser: CoinGeckoLinkParser

    val assetIconProvider: AssetIconProvider

    val swapFlowScopeAggregator: SwapFlowScopeAggregator

    val okHttpClient: OkHttpClient

    val mythosMainPotMatcherFactory: MythosMainPotMatcherFactory

    val bannerSourceFactory: BannersSourceFactory

    val bannersMixinFactory: PromotionBannersMixinFactory

    val webViewPermissionAskerFactory: WebViewPermissionAskerFactory

    val webViewFileChooserFactory: WebViewFileChooserFactory

    val tradeTokenRegistry: TradeTokenRegistry

    val interceptingWebViewClientFactory: InterceptingWebViewClientFactory

    val mercuryoSellRequestInterceptorFactory: MercuryoSellRequestInterceptorFactory

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

    fun proxyPriceApi(): ProxyPriceApi

    fun coingeckoApi(): CoingeckoApi

    fun assetsViewModeRepository(): AssetsViewModeRepository

    fun walletConnectSessionsUseCase(): WalletConnectSessionsUseCase

    fun assetsIconModeRepository(): AssetsIconModeRepository

    fun nftRepository(): NftRepository

    fun systemCallExecutor(): SystemCallExecutor

    fun permissionsAskerFactory(): PermissionsAskerFactory

    fun assetViewModeInteractor(): AssetViewModeInteractor

    fun maxActionProviderFactory(): MaxActionProviderFactory

    fun copyAddressMixin(): CopyAddressMixin
}
