package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v1.response

class ReferendumDetailsV1Response(
    val title: String?,
    val content: String?,
    val author: Author?,
    val onchainData: OnChainData
) {

    class Author(val username: String?, val address: String?)

    class OnChainData(val timeline: List<Status>)

    class Status(
        val indexer: IndexerState,
        val method: String
    )

    class IndexerState(
        val blockTime: Long
    )
}
