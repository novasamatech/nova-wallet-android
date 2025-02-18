package io.novafoundation.nova.feature_assets.presentation.views.priceCharts

import android.graphics.Color
import android.graphics.Typeface
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.feature_assets.R
import kotlinx.android.synthetic.main.view_price_charts.view.priceChart

class ChartController(private val chart: LineChart, private val callback: Callback) {

    interface Callback {
        fun onSelectEntry(startEntry: Entry, selectedEntry: Entry, isLastEntry: Boolean)
    }

    private val context = chart.context

    private var currentEntries: List<Entry> = emptyList()
    private var useNeutralColor = false

    init {
        setupChartUI()
    }

    fun showYAxis(show: Boolean) {
        chart.axisRight.isEnabled = show
    }

    fun useNeutralColor(useNeutral: Boolean) {
        this.useNeutralColor = useNeutral
        updateChart()
    }

    fun setEntries(entries: List<Entry>) {
        currentEntries = entries
        updateChart()
    }

    private fun setupChartUI() {
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisLeft.isEnabled = false
        chart.xAxis.isEnabled = false
        chart.setScaleEnabled(false)
        chart.axisRight.typeface

        chart.renderer = PriceChartRenderer(
            highlightColor = context.getColor(R.color.neutral_price_chart_line),
            dotRadius = 4.dpF(context),
            strokeWidth = 4.dpF(context),
            strokeAlpha = 0.16f,
            chart = chart
        )

        chart.axisRight.apply {
            isEnabled = true
            textSize = 9f
            textColor = context.getColor(R.color.text_secondary)
            typeface = Typeface.MONOSPACE
            gridLineWidth = 1.5f
            setDrawAxisLine(false)
            setDrawGridLines(true)
            gridColor = context.getColor(R.color.price_chart_grid_line)
            enableGridDashedLine(10f, 10f, 0f)
        }

        chart.marker = null
        chart.onChartGestureListener = PriceChartGestureListener(
            onGestureStart = {
                val highlight = chart.getHighlightByTouchPoint(it.x, it.y)
                chart.highlightValue(highlight)
                val entry = chart.data?.getEntryForHighlight(highlight)
                entry?.let { updateChartWithSelectedEntry(it) }
            },
            onGestureEnd = {
                chart.highlightValue(null)
                updateChart()
            }
        )

        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, h: Highlight) {
                updateChartWithSelectedEntry(e)
            }

            override fun onNothingSelected() {
                updateChart()
            }
        })
    }

    private fun updateChartWithSelectedEntry(entry: Entry) {
        val entriesBefore = currentEntries.filter { it.x <= entry.x }
        val entriesAfter = currentEntries.subList(entriesBefore.size - 1, currentEntries.size)

        val beforeEntriesColor = entriesBefore.getColorResForEntries()
        val datasetBefore = entriesBefore.createDataSet(beforeEntriesColor)
        val datasetAfter = entriesAfter.createDataSet(R.color.neutral_price_chart_line)

        chart.priceChartRenderer().apply {
            setDotPoint(entry)
            setDotColor(context.getColor(beforeEntriesColor))
        }

        chart.data = LineData(datasetBefore, datasetAfter)
        chart.invalidate()

        onSelectEntry(entriesBefore, isLastEntry = false)
    }

    private fun updateChart() {
        chart.priceChartRenderer().apply {
            setDotPoint(null)
            setDotColor(null)
        }

        val dataSet = currentEntries.createDataSet(currentEntries.getColorResForEntries())
        chart.data = LineData(dataSet)
        chart.invalidate()

        onSelectEntry(currentEntries, isLastEntry = true)
    }

    private fun onSelectEntry(entries: List<Entry>, isLastEntry: Boolean) {
        if (entries.isEmpty()) return

        callback.onSelectEntry(entries.first(), entries.last(), isLastEntry)
    }

    private fun List<Entry>.createDataSet(colorRes: Int): LineDataSet {
        return LineDataSet(this, "").apply {
            color = context.getColor(colorRes)
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            lineWidth = 1.5f
        }
    }

    private fun LineChart.priceChartRenderer() = priceChart.renderer as PriceChartRenderer

    private fun List<Entry>.getColorResForEntries(): Int {
        if (useNeutralColor) return R.color.neutral_price_chart_line

        return if (isBullish()) R.color.positive_price_chart_line else R.color.negative_price_chart_line
    }

    private fun List<Entry>.isBullish(): Boolean {
        return last().y >= first().y
    }
}
