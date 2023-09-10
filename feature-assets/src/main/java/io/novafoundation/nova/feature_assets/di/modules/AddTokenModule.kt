package io.novafoundation.nova.feature_assets.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.format.EthereumAddressFormat
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_assets.domain.tokens.add.AddTokensInteractor
import io.novafoundation.nova.feature_assets.domain.tokens.add.CoinGeckoLinkParser
import io.novafoundation.nova.feature_assets.domain.tokens.add.RealAddTokensInteractor
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.CoinGeckoLinkValidationFactory
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_wallet_api.data.network.coingecko.CoingeckoApi
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class AddTokenModule {

    @Provides
    fun coinGeckoLinkValidationFactory(
        coinGeckoApi: CoingeckoApi,
        coinGeckoLinkParser: CoinGeckoLinkParser
    ): CoinGeckoLinkValidationFactory {
        return CoinGeckoLinkValidationFactory(coinGeckoApi, coinGeckoLinkParser)
    }

    @Provides
    @FeatureScope
    fun provideInteractor(
        chainRegistry: ChainRegistry,
        erc20Standard: Erc20Standard,
        chainAssetRepository: ChainAssetRepository,
        coinGeckoLinkParser: CoinGeckoLinkParser,
        ethereumAddressFormat: EthereumAddressFormat,
        currencyRepository: CurrencyRepository,
        walletRepository: WalletRepository,
        coinGeckoLinkValidationFactory: CoinGeckoLinkValidationFactory,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorage: StorageDataSource
    ): AddTokensInteractor {
        return RealAddTokensInteractor(
            chainRegistry,
            erc20Standard,
            chainAssetRepository,
            coinGeckoLinkParser,
            ethereumAddressFormat,
            currencyRepository,
            walletRepository,
            coinGeckoLinkValidationFactory,
            remoteStorage
        )
    }
}
