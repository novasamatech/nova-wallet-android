package io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters

import android.widget.TextView
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.formatDateSinceEpoch
import io.novafoundation.nova.feature_assets.R
import java.util.Date
import kotlin.time.Duration.Companion.seconds

interface DateChartTextInjector {
    fun format(timestamp: Long, isLastEntry: Boolean, textView: TextView)
}

class RealDateChartTextInjector(
    private val resourceManager: ResourceManager
) : DateChartTextInjector {

    override fun format(timestamp: Long, isLastEntry: Boolean, textView: TextView) {
        if (isLastEntry) {
            textView.text = resourceManager.getString(R.string.today)
            return
        } else {
            val date = Date(timestamp.seconds.inWholeMilliseconds)
            val dateString = date.formatDateSinceEpoch(resourceManager)
            val timeString = resourceManager.formatTime(date.time)

            textView.text = resourceManager.getString(R.string.price_chart_date_format, dateString, timeString)
        }
    }
}
