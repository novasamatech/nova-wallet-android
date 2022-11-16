package io.novafoundation.nova.runtime.multiNetwork.chain.remote.model

class ChainExternalApiRemote(
    val staking: Section?,
    val history: List<TransferApi>,
    val crowdloans: Section?,
    val governance: Section?
) {

    class Section(val type: String, val url: String)

    class TransferApi(
        val type: String,
        val url: String,
        val assetType: String?
    )
}
