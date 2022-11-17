package io.novafoundation.nova.runtime.multiNetwork.chain.remote.model

data class ChainExternalApiRemote(
    val staking: Section?,
    val history: List<TransferApi>,
    val crowdloans: Section?,
    val governance: Section?
) {

    class Section(val type: String, val url: String)

    data class TransferApi(
        val type: String,
        val url: String,
        val assetType: String?
    )
}
