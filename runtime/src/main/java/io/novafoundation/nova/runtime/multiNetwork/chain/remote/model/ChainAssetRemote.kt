package io.novafoundation.nova.runtime.multiNetwork.chain.remote.model

class ChainAssetRemote(
    val assetId: Int,
    val symbol: String,
    val precision: Int,
    val priceId: String?,
    val name: String?,
    val staking: String?,
    val type: String?,
    val icon: String?,
    val buyProviders: Map<String, Map<String, Any>>?, // { "providerName": { arguments map } }
    val typeExtras: Map<String, Any?>?,
)
