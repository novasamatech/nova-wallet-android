package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance.source

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class BalanceSourceProvider(
    private val nativeBalanceSource: NativeBalanceSource,
    private val statemineBalanceSource: StatemineBalanceSource,
    private val unsupportedBalanceSource: UnsupportedBalanceSource
) {

    fun provideFor(asset: Chain.Asset): BalanceSource {
        return when(asset.type) {
            is Chain.Asset.Type.Native -> nativeBalanceSource
            is Chain.Asset.Type.Statemine -> statemineBalanceSource
            Chain.Asset.Type.Unsupported -> unsupportedBalanceSource
        }
    }
}
