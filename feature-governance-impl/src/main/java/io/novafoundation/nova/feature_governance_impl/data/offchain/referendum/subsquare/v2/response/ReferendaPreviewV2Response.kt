package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v2.response

import java.math.BigInteger

class ReferendaPreviewV2Response(
    val items: List<Referendum>
) {

    class Referendum(
        val title: String?,
        val referendumIndex: BigInteger
    )
}
