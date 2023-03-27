package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.response

import com.google.gson.annotations.SerializedName
import java.math.BigInteger

class ReferendumDetailsResponse(
    val posts: List<Post>
) {

    class Post(
        val title: String?,
        val content: String,
        val author: Author,
        @SerializedName("onchain_link") val onchainLink: OnChainLink?
    )

    class Author(val username: String)

    class OnChainLink(
        @SerializedName("onchain_referendum") val onchainReferendum: List<OnChainReferendum>?,
        @SerializedName("proposer_address") val proposerAddress: String,
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
