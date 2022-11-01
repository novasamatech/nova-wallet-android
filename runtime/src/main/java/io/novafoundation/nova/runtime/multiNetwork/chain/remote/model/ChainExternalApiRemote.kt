package io.novafoundation.nova.runtime.multiNetwork.chain.remote.model

class ChainExternalApiRemote(
    val staking: Section?,
    val history: Section?,
    val crowdloans: Section?,
    val governance: Section?
) {

    class Section(val type: String, val url: String)
}
