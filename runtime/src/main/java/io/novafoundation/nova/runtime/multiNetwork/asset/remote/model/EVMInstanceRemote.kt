package io.novafoundation.nova.runtime.multiNetwork.asset.remote.model

class EVMInstanceRemote(
    val chainId: String,
    val contractAddress: String,
    val buyProviders: Map<String, Map<String, Any>>?,
    val sellProviders: Map<String, Map<String, Any>>?
)
