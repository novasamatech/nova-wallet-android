package io.novafoundation.nova.feature_wallet_api.di

import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.BalanceLocksUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.PaymentUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainValidationSystemProvider
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.data.network.priceApi.CoingeckoApi
import io.novafoundation.nova.feature_wallet_api.data.network.priceApi.ProxyPriceApi
import io.novafoundation.nova.feature_wallet_api.data.repository.AccountInfoRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceHoldsRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.ExternalBalanceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.StatemineAssetsRepository
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.MultisigExtrinsicValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.ProxyHaveEnoughFeeValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.maskable.MaskableValueFormatterFactory
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.maskable.MaskableValueFormatterProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard

interface WalletFeatureApi {

    val maskableValueFormatterFactory: MaskableValueFormatterFactory

    val amountFormatterProvider: MaskableValueFormatterProvider

    val assetSourceRegistry: AssetSourceRegistry

    val phishingValidationFactory: PhishingValidationFactory

    val crossChainTransfersRepository: CrossChainTransfersRepository

    val crossChainWeigher: CrossChainWeigher

    val crossChainTransactor: CrossChainTransactor

    val crossChainValidationSystemProvider: CrossChainValidationSystemProvider

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

    val feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory

    val assetsValidationContextFactory: AssetsValidationContext.Factory

    val statemineAssetsRepository: StatemineAssetsRepository

    val multisigExtrinsicValidationFactory: MultisigExtrinsicValidationFactory

    val accountInfoRepository: AccountInfoRepository

    val amountFormatter: AmountFormatter

    fun provideWalletRepository(): WalletRepository

    fun provideTokenRepository(): TokenRepository

    fun provideAssetCache(): AssetCache

    fun provideWallConstants(): WalletConstants

    fun provideFeeLoaderMixinFactory(): FeeLoaderMixin.Factory

    fun provideAmountChooserFactory(): AmountChooserMixin.Factory

    fun proxyPriceApi(): ProxyPriceApi

    fun coingeckoApi(): CoingeckoApi

    fun enoughTotalToStayAboveEDValidationFactory(): EnoughTotalToStayAboveEDValidationFactory

    fun proxyHaveEnoughFeeValidationFactory(): ProxyHaveEnoughFeeValidationFactory

    fun maxActionProviderFactory(): MaxActionProviderFactory
}
