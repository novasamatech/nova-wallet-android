package io.novafoundation.nova.feature_account_api.data.mappers

import android.graphics.Color
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.GradientUi
import io.novafoundation.nova.runtime.multiNetwork.ChainGradientParser
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

private val DEFAULT_GRADIENT by lazy {
    ChainGradientParser.parse("linear-gradient(135deg, #F2A007 19.29%, #A56B00 100%)")!!
}

fun mapChainToUi(chain: Chain): ChainUi = with(chain) {
    ChainUi(
        id = id,
        name = name,
        gradient = mapGradientToUi(chain.color ?: DEFAULT_GRADIENT),
        icon = icon
    )
}

private fun mapGradientToUi(gradient: Chain.Gradient) = GradientUi(
    angle = gradient.angle,
    colors = gradient.colors.map(Color::parseColor).toIntArray(),
    positions = gradient.positions.toFloatArray()
)
