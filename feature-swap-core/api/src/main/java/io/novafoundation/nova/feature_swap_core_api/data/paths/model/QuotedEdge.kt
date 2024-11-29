package io.novafoundation.nova.feature_swap_core_api.data.paths.model

import java.math.BigInteger

class QuotedEdge<E>(
    val quotedAmount: BigInteger,
    val quote: BigInteger,
    val edge: E
)
