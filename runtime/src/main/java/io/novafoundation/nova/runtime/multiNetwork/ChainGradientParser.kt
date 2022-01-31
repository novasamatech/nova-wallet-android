package io.novafoundation.nova.runtime.multiNetwork

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

/**
 * Definition format: linear-gradient(<degrees>deg, #<color1> <percent1>%, ...);
 */
object ChainGradientParser {

    private val MAIN_REGEX by lazy {
        "linear-gradient\\(([0-9.]*)deg,([^)]+)\\)".toRegex()
    }

    private val COLORS_REGEX by lazy {
        "\\s?(#[0-9A-F]*) ([0-9.]*)%".toRegex()
    }

    fun parse(definition: String?): Chain.Gradient? {
        if (definition == null) return null

        return runCatching {

            val (_, degreeRaw, colorsAndPositionsRaw) = MAIN_REGEX.find(definition)!!.groupValues
            val colorsAndPositions = COLORS_REGEX.findAll(colorsAndPositionsRaw).map { match ->
                match.groupValues[1] to match.groupValues[2]
            }.toList()

            val colors = colorsAndPositions.map { it.first }
            val positions = colorsAndPositions.map { it.second.toFloat() }

            Chain.Gradient(
                angle = degreeRaw.toFloat(),
                colors = colors,
                positionsPercent = positions
            )
        }.getOrThrow()
    }

    fun encode(gradient: Chain.Gradient?): String? = gradient?.let {
        gradient.colors.zip(gradient.positionsPercent).joinToString(
            prefix = "linear-gradient(${gradient.angle}deg, ",
            separator = ", ",
            postfix = ")"
        ) { (color, position) ->
            "$color $position%"
        }
    }
}
