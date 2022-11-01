package io.novafoundation.nova.feature_governance_impl.data.offchain.remote.model.response

import java.math.BigInteger

class ReferendumDetailsResponse(
    val posts: List<Post>
) {

    class Post(
        val title: String,
        val content: String,
        val author: Author,
        val onchain_link: OnChainLink
    )

    class Author(val username: String)

    class OnChainLink(
        val onchain_referendum: List<OnChainReferendum>
    )

    class OnChainReferendum(
        val referendumStatus: List<Status>
    )

    class Status(
        val blockNumber: BlockNumber,
        val status: String
    )

    class BlockNumber(
        val startDateTime: String,
        val number: BigInteger
    )
}
