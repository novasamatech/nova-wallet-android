package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.EvmTransactionService
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.StaticAssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.evnNative.EvmNativeAssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.evmNative.EvmNativeAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.evmNative.EvmNativeAssetTransfers
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.EtherscanTransactionsApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Qualifier

@Qualifier
annotation class EvmNativeAssets

@Module
class EvmNativeAssetsModule {

    @Provides
    @FeatureScope
    fun provideBalance(
        assetCache: AssetCache,
        chainRegistry: ChainRegistry,
    ) = EvmNativeAssetBalance(assetCache, chainRegistry)

    @Provides
    @FeatureScope
    fun provideTransfers(
        evmTransactionService: EvmTransactionService,
        assetSourceRegistry: AssetSourceRegistry
    ) = EvmNativeAssetTransfers(evmTransactionService, assetSourceRegistry)

    @Provides
    @FeatureScope
    fun provideHistory(
        chainRegistry: ChainRegistry,
        etherscanTransactionsApi: EtherscanTransactionsApi,
        coinPriceRepository: CoinPriceRepository
    ) = EvmNativeAssetHistory(chainRegistry, etherscanTransactionsApi, coinPriceRepository)

    @Provides
    @EvmNativeAssets
    @FeatureScope
    fun provideAssetSource(
        evmNativeAssetBalance: EvmNativeAssetBalance,
        evmNativeAssetTransfers: EvmNativeAssetTransfers,
        evmNativeAssetHistory: EvmNativeAssetHistory,
    ): AssetSource = StaticAssetSource(
        transfers = evmNativeAssetTransfers,
        balance = evmNativeAssetBalance,
        history = evmNativeAssetHistory
    )
}
