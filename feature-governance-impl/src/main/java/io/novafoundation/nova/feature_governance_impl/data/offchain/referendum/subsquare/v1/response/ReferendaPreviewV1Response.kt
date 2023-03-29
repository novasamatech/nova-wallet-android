package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v1.response

import java.math.BigInteger

class ReferendaPreviewV1Response(
    val items: List<Referendum>
) {

    class Referendum(
        val title: String?,
        val referendumIndex: BigInteger
    )
}
