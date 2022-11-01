package io.novafoundation.nova.feature_governance_impl.data.offchain.remote.model.response

import java.math.BigInteger

class ReferendaPreviewResponse(
    val posts: List<Post>
) {

    class Post(
        val id: BigInteger,
        val title: String
    )
}
