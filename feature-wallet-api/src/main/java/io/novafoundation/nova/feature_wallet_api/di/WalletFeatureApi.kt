package io.novafoundation.nova.feature_wallet_api.di

import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.coingecko.CoingeckoApi
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.implementations.CoinPriceInteractor
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionHistoryRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard

interface WalletFeatureApi {

    fun provideWalletRepository(): WalletRepository

    fun provideTokenRepository(): TokenRepository

    fun provideAssetCache(): AssetCache

    fun provideWallConstants(): WalletConstants

    @Wallet
    fun provideWalletUpdateSystem(): UpdateSystem

    fun provideFeeLoaderMixinFactory(): FeeLoaderMixin.Factory

    fun provideCoinPriceRateInteractor(): CoinPriceInteractor

    val assetSourceRegistry: AssetSourceRegistry

    fun provideAmountChooserFactory(): AmountChooserMixin.Factory

    fun coingeckoApi(): CoingeckoApi

    val phishingValidationFactory: PhishingValidationFactory

    val crossChainTransfersRepository: CrossChainTransfersRepository
    val crossChainWeigher: CrossChainWeigher
    val crossChainTransactor: CrossChainTransactor

    val balanceLocksRepository: BalanceLocksRepository

    val transactionHistoryRepository: TransactionHistoryRepository

    val chainAssetRepository: ChainAssetRepository

    val erc20Standard: Erc20Standard
}
