package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances

import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml.OrmlBalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine.StatemineBalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.utility.NativeBalanceSource
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class BalanceSourceProvider(
    private val nativeBalanceSource: NativeBalanceSource,
    private val statemineBalanceSource: StatemineBalanceSource,
    private val ormlBalanceSource: OrmlBalanceSource,
    private val unsupportedBalanceSource: UnsupportedBalanceSource
) {

    fun provideFor(asset: Chain.Asset): BalanceSource {
        return when (asset.type) {
            is Chain.Asset.Type.Native -> nativeBalanceSource
            is Chain.Asset.Type.Statemine -> statemineBalanceSource
            is Chain.Asset.Type.Orml -> ormlBalanceSource
            Chain.Asset.Type.Unsupported -> unsupportedBalanceSource
        }
    }
}
