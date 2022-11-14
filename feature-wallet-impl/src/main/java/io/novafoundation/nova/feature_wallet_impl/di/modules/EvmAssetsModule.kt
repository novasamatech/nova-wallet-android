package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.StaticAssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.evm.EvmAssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.evm.EvmAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.evm.EvmAssetTransfers
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Qualifier

@Qualifier
annotation class EvmAssets

@Module
class EvmAssetsModule {

    @Provides
    @FeatureScope
    fun provideErc20Standard() = Erc20Standard()

    @Provides
    @FeatureScope
    fun provideBalance(
        chainRegistry: ChainRegistry,
        assetCache: AssetCache,
        erc20Standard: Erc20Standard,
    ) = EvmAssetBalance(chainRegistry, assetCache, erc20Standard)

    @Provides
    @FeatureScope
    fun provideTransfers() = EvmAssetTransfers()

    @Provides
    @FeatureScope
    fun provideHistory() = EvmAssetHistory()

    @Provides
    @EvmAssets
    @FeatureScope
    fun provideAssetSource(
        evmAssetBalance: EvmAssetBalance,
        evmAssetTransfers: EvmAssetTransfers,
        evmAssetHistory: EvmAssetHistory,
    ): AssetSource = StaticAssetSource(
        transfers = evmAssetTransfers,
        balance = evmAssetBalance,
        history = evmAssetHistory
    )
}
