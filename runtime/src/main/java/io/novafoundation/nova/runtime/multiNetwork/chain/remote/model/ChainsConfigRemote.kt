package io.novafoundation.nova.runtime.multiNetwork.chain.remote.model

class ChainsConfigRemote(
    val chains: List<ChainRemote>,
    val assetDisplayPriorities: Map<String, Int>,
    val chainDisplayPriorities: Map<String, Int>
)
