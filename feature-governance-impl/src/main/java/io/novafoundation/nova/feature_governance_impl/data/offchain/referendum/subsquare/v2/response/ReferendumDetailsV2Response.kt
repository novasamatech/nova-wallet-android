package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v2.response

class ReferendumDetailsV2Response(
    val title: String?,
    val content: String?,
    val author: Author?,
    val onchainData: OnChainData
) {

    class Author(val username: String?, val address: String?)

    class OnChainData(val timeline: List<Status>)

    class Status(
        val indexer: IndexerState,
        val name: String
    )

    class IndexerState(
        val blockTime: Long
    )
}
