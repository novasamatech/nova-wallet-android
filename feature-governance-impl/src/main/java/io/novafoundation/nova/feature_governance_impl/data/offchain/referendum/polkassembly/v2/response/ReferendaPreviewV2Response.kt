package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2.response

import com.google.gson.annotations.SerializedName
import java.math.BigInteger

class ReferendaPreviewV2Response(
    val posts: List<Post>
) {

    class Post(
        val title: String?,
        @SerializedName("onchain_link") val onChainLink: OnChainLink
    )

    class OnChainLink(@SerializedName("onchain_referendumv2_id") val onChainReferendumId: BigInteger)
}
