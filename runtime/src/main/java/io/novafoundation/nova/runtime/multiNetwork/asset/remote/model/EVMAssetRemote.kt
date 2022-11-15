package io.novafoundation.nova.runtime.multiNetwork.asset.remote.model

data class EVMAssetRemote(
    val symbol: String,
    val precision: Int,
    val priceId: String?,
    val name: String,
    val icon: String?,
    val instances: List<EVMInstanceRemote>
)
