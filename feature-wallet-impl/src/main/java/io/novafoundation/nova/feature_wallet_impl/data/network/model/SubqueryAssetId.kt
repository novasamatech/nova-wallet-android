package io.novafoundation.nova.feature_wallet_impl.data.network.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.StatemineAssetId

fun Chain.Asset.Type.subQueryAssetId(): String {
    return when (this) {
        is Chain.Asset.Type.Equilibrium -> id.toString()
        Chain.Asset.Type.Native -> "native"
        is Chain.Asset.Type.Orml -> currencyIdScale
        is Chain.Asset.Type.Statemine -> id.subQueryAssetId()
        else -> error("Unsupported assetId type for SubQuery request: ${this::class.simpleName}")
    }
}

fun Chain.Asset.Type.isSupportedBySubQuery(): Boolean {
    return when (this) {
        is Chain.Asset.Type.Equilibrium,
        Chain.Asset.Type.Native,
        is Chain.Asset.Type.Orml,
        is Chain.Asset.Type.Statemine -> true

        else -> false
    }
}

fun StatemineAssetId.subQueryAssetId(): String {
    return when (this) {
        is StatemineAssetId.Number -> value.toString()
        is StatemineAssetId.ScaleEncoded -> scaleHex
    }
}

typealias AssetsBySubQueryId = Map<String?, Chain.Asset>

fun Chain.assetsBySubQueryId(): AssetsBySubQueryId {
    return assets
        .filter { it.type.isSupportedBySubQuery() }
        .associateBy { it.type.subQueryAssetId() }
}
