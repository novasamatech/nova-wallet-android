package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.response

import com.google.gson.annotations.SerializedName
import java.math.BigInteger

class ReferendaPreviewResponse(
    val posts: List<Post>
) {

    class Post(
        val title: String?,
        @SerializedName("onchain_link") val onChainLink: OnChainLink
    )

    class OnChainLink(
        @SerializedName("onchain_referendum_id") val onChainReferendumId: BigInteger,
    )
}
