package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.response

import java.math.BigInteger

fun ParachainReferendaPreviewResponse.Post.getId(): BigInteger {
    return onChainLink.onChainReferendum[0].referendumId
}

fun ReferendaPreviewResponse.Post.getId(): BigInteger {
    return onChainLink.onChainReferendumId
}
