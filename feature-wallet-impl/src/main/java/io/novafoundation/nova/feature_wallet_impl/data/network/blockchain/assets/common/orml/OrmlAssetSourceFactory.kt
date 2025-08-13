package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.common.orml

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.StaticAssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml.OrmlAssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml.hydrationEvm.HydrationEvmOrmlAssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.orml.OrmlAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.orml.OrmlAssetTransfers
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

@FeatureScope
class OrmlAssetSourceFactory @Inject constructor(
    defaultBalance: OrmlAssetBalance,
    defaultHistory: OrmlAssetHistory,
    defaultTransfers: OrmlAssetTransfers,

    hydrationEvmOrmlAssetBalance: HydrationEvmOrmlAssetBalance,
) {

    private val defaultSource = StaticAssetSource(defaultTransfers, defaultBalance, defaultHistory)
    private val hydrationEvmSource = StaticAssetSource(defaultTransfers, hydrationEvmOrmlAssetBalance, defaultHistory)

    fun allSources(): List<AssetSource> {
        return listOf(defaultSource, hydrationEvmSource)
    }

    fun getSourceBySubtype(subType: Chain.Asset.Type.Orml.SubType): AssetSource {
        return when (subType) {
            Chain.Asset.Type.Orml.SubType.DEFAULT -> defaultSource
            Chain.Asset.Type.Orml.SubType.HYDRATION_EVM -> hydrationEvmSource
        }
    }
}
