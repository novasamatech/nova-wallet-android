package io.novafoundation.nova.feature_governance_impl.presentation.common.conviction

import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.view.input.seekbar.SeekbarValue
import io.novafoundation.nova.common.view.input.seekbar.SeekbarValues
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amountMultiplier
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction

interface ConvictionValuesProvider {

    fun convictionValues(): SeekbarValues<Conviction>
}

class RealConvictionValuesProvider : ConvictionValuesProvider {

    override fun convictionValues(): SeekbarValues<Conviction> {
        val colors = listOf(
            R.color.conviction_slider_text_01x,
            R.color.conviction_slider_text_1x,
            R.color.conviction_slider_text_2x,
            R.color.conviction_slider_text_3x,
            R.color.conviction_slider_text_4x,
            R.color.conviction_slider_text_5x,
            R.color.conviction_slider_text_6x
        )

        val values = Conviction.values().mapIndexed { index, conviction ->
            SeekbarValue(
                value = conviction,
                label = "${conviction.amountMultiplier().format()}x",
                labelColorRes = colors[index]
            )
        }

        return SeekbarValues(values)
    }
}
