package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.EvmTransactionService
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.StaticAssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.evmErc20.EvmErc20AssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.evmErc20.EvmErc20AssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.evmErc20.EvmErc20AssetTransfers
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.EtherscanApiKeys
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.EtherscanTransactionsApi
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.RealEtherscanTransactionsApi
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.RetrofitEtherscanTransactionsApi
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Qualifier

@Qualifier
annotation class EvmErc20Assets

@Module
class EvmErc20AssetsModule {

    @Provides
    @FeatureScope
    fun provideErc20Standard() = Erc20Standard()

    @Provides
    @FeatureScope
    fun provideBalance(
        chainRegistry: ChainRegistry,
        assetCache: AssetCache,
        erc20Standard: Erc20Standard,
    ) = EvmErc20AssetBalance(chainRegistry, assetCache, erc20Standard)

    @Provides
    @FeatureScope
    fun provideTransfers(
        evmTransactionService: EvmTransactionService,
        erc20Standard: Erc20Standard,
        assetSourceRegistry: AssetSourceRegistry
    ) = EvmErc20AssetTransfers(evmTransactionService, erc20Standard, assetSourceRegistry)

    @Provides
    @FeatureScope
    fun provideEtherscanTransfersApi(
        networkApiCreator: NetworkApiCreator
    ): RetrofitEtherscanTransactionsApi = networkApiCreator.create(RetrofitEtherscanTransactionsApi::class.java)

    @Provides
    @FeatureScope
    fun provideEtherscanApiKeys() = EtherscanApiKeys.default()

    @Provides
    @FeatureScope
    fun provideEtherscanTransactionsApi(
        retrofitEtherscanTransactionsApi: RetrofitEtherscanTransactionsApi,
        etherscanApiKeys: EtherscanApiKeys,
    ): EtherscanTransactionsApi = RealEtherscanTransactionsApi(retrofitEtherscanTransactionsApi, etherscanApiKeys)

    @Provides
    @FeatureScope
    fun provideHistory(
        etherscanTransactionsApi: EtherscanTransactionsApi
    ) = EvmErc20AssetHistory(etherscanTransactionsApi)

    @Provides
    @EvmErc20Assets
    @FeatureScope
    fun provideAssetSource(
        evmErc20AssetBalance: EvmErc20AssetBalance,
        evmErc20AssetTransfers: EvmErc20AssetTransfers,
        evmErc20AssetHistory: EvmErc20AssetHistory,
    ): AssetSource = StaticAssetSource(
        transfers = evmErc20AssetTransfers,
        balance = evmErc20AssetBalance,
        history = evmErc20AssetHistory
    )
}
