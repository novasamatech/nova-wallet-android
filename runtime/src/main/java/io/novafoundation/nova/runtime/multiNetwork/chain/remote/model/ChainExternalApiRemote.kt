package io.novafoundation.nova.runtime.multiNetwork.chain.remote.model

data class ChainExternalApiRemote(
    val staking: Section?,
    val history: List<TransferApi>,
    val crowdloans: Section?,
    val governance: GovernanceSection?
) {

    class Section(val type: String, val url: String)

    class GovernanceSection(val type: String, val url: String, val parameters: Map<String, Any?>)

    data class TransferApi(
        val type: String,
        val url: String,
        val assetType: String?
    )
}
