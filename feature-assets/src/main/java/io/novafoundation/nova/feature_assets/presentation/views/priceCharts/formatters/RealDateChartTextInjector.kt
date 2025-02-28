package io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters

import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.formatDateSinceEpoch
import io.novafoundation.nova.common.utils.formatting.isThisYear
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.views.priceCharts.PriceChartModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.seconds

interface DateChartTextInjector {
    fun format(timestamp: Long, isEntrySelected: Boolean, textView: TextView, priceChartModel: PriceChartModel)
}

class RealDateChartTextInjector(
    private val resourceManager: ResourceManager
) : DateChartTextInjector {

    override fun format(timestamp: Long, isEntrySelected: Boolean, textView: TextView, priceChartModel: PriceChartModel) {
        val chartModel = priceChartModel as? PriceChartModel.Chart ?: return

        if (isEntrySelected) {
            val date = Date(timestamp.seconds.inWholeMilliseconds)
            val dateString = formatDate(textView, date)

            textView.text = if (chartModel.supportTimeShowing) {
                val timeString = resourceManager.formatTime(date.time)
                resourceManager.getString(R.string.price_chart_date_format, dateString, timeString)
            } else {
                dateString
            }
        } else {
            textView.text = chartModel.periodName
        }
    }

    private fun formatDate(view: View, date: Date): String {
        return if (date.isThisYear()) {
            formatDateThisYear(view, date.time)
        } else {
            formatDateOtherYear(view, date.time)
        }
    }

    private fun formatDateThisYear(view: View, timestamp: Long): String {
        return DateUtils.formatDateTime(
            view.context,
            timestamp,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_MONTH
        )
    }

    private fun formatDateOtherYear(view: View, timestamp: Long): String {
        return DateUtils.formatDateTime(
            view.context,
            timestamp,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
        )
    }
}
