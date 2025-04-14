package io.novafoundation.nova.runtime.multiNetwork.chain.remote.model

data class ChainAssetRemote(
    val assetId: Int,
    val symbol: String,
    val precision: Int,
    val priceId: String?,
    val name: String?,
    val staking: List<String>?,
    val type: String?,
    val icon: String?,
    val buyProviders: Map<String, Map<String, Any>>?, // { "providerName": { arguments map } }
    val sellProviders: Map<String, Map<String, Any>>?, // { "providerName": { arguments map } }
    val typeExtras: Map<String, Any?>?,
)
