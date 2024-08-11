package io.novafoundation.nova.feature_wallet_api.di

import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.BalanceLocksUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.PaymentUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.coingecko.CoingeckoApi
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceHoldsRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.ExternalBalanceRepository
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.ProxyHaveEnoughFeeValidationFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard

interface WalletFeatureApi {

    fun provideWalletRepository(): WalletRepository

    fun provideTokenRepository(): TokenRepository

    fun provideAssetCache(): AssetCache

    fun provideWallConstants(): WalletConstants

    fun provideFeeLoaderMixinFactory(): FeeLoaderMixin.Factory

    val assetSourceRegistry: AssetSourceRegistry

    fun provideAmountChooserFactory(): AmountChooserMixin.Factory

    fun coingeckoApi(): CoingeckoApi

    fun enoughTotalToStayAboveEDValidationFactory(): EnoughTotalToStayAboveEDValidationFactory

    fun proxyHaveEnoughFeeValidationFactory(): ProxyHaveEnoughFeeValidationFactory

    val phishingValidationFactory: PhishingValidationFactory

    val crossChainTransfersRepository: CrossChainTransfersRepository
    val crossChainWeigher: CrossChainWeigher
    val crossChainTransactor: CrossChainTransactor

    val balanceLocksRepository: BalanceLocksRepository

    val chainAssetRepository: ChainAssetRepository

    val erc20Standard: Erc20Standard

    val arbitraryAssetUseCase: ArbitraryAssetUseCase

    val externalBalancesRepository: ExternalBalanceRepository

    val paymentUpdaterFactory: PaymentUpdaterFactory

    val balanceLocksUpdaterFactory: BalanceLocksUpdaterFactory

    val coinPriceRepository: CoinPriceRepository

    val crossChainTransfersUseCase: CrossChainTransfersUseCase

    val arbitraryTokenUseCase: ArbitraryTokenUseCase

    val holdsRepository: BalanceHoldsRepository
}
