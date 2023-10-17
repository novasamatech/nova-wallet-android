package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.Percent

object Slippage {

    val DEFAULT = Percent(0.5)

    val SUGGESTIONS = listOf(Percent(0.1), Percent(1.0), Percent(3.0))
}
