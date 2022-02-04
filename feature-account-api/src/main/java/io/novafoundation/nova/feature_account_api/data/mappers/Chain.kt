package io.novafoundation.nova.feature_account_api.data.mappers

import android.graphics.Color
import io.novafoundation.nova.common.utils.floorMod
import io.novafoundation.nova.common.utils.percentageToFraction
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.GradientUi
import io.novafoundation.nova.runtime.multiNetwork.ChainGradientParser
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

private val DEFAULT_GRADIENT by lazy {
    ChainGradientParser.parse("linear-gradient(315deg, #434852 0%, #787F92 100%)")!!
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
    angle = cssAngleToAndroid(gradient.angle.toInt()),
    colors = gradient.colors.map(Color::parseColor).toIntArray(),
    positions = gradient.positionsPercent.map(Float::percentageToFraction).toFloatArray()
)

/**
 * Given the following coordinate system
 *          N
 *          |
 *          |
 *          |
 * W--------  --------- E
 *          |
 *          |
 *          |
 *          S
 * Css starts from N clockwise
 * Android starts from E counter-clockwise
 */
fun cssAngleToAndroid(angle: Int) = (-angle + 90) floorMod 360
