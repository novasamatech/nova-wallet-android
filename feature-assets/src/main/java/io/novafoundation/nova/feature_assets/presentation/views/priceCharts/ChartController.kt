package io.novafoundation.nova.feature_assets.presentation.views.priceCharts

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.view.MotionEvent
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import io.novafoundation.nova.common.utils.binarySearchFloor
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.feature_assets.R
import kotlinx.android.synthetic.main.view_price_charts.view.priceChart

class ChartController(private val chart: LineChart, private val callback: Callback) {

    interface Callback {
        fun onSelectEntry(startEntry: Entry, selectedEntry: Entry, isEntrySelected: Boolean)
    }

    private val context = chart.context

    private val chartUIParams = ChartUIParams.default(context)

    private var currentEntries: List<Entry> = emptyList()
    private var useNeutralColor = false

    init {
        setupChartUI()
    }

    fun showYAxis(show: Boolean) {
        chart.axisRight.setDrawLabels(show)
        chart.invalidate()
    }

    fun useNeutralColor(useNeutral: Boolean) {
        this.useNeutralColor = useNeutral
        updateChart()
    }

    fun setEntries(entries: List<Entry>) {
        currentEntries = entries
        updateChart()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupChartUI() {
        val chartUIParams = getSharedChartUIParams(context)

        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisLeft.isEnabled = false
        chart.xAxis.isEnabled = false
        chart.setScaleEnabled(false)
        chart.minOffset = 0f
        chart.extraTopOffset = 12f
        chart.extraBottomOffset = 12f
        chart.isHighlightPerTapEnabled = false
        chart.isHighlightPerDragEnabled = false
        chart.marker = null

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
            setLabelCount(chartUIParams.gridLines, true)
            setDrawTopYLabelEntry(true)
            gridLineWidth = chartUIParams.gridLineWidthDp
            setDrawAxisLine(false)
            setDrawGridLines(true)
            gridColor = context.getColor(R.color.price_chart_grid_line)
            setGridDashedLine(chartUIParams.gridLineDashEffect)
        }

        chart.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                    val entry = chart.getEntryByTouchPoint(event)
                    if (entry != null) {
                        updateChartWithSelectedEntry(entry)
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    updateChart()
                }
            }

            false
        }
    }

    private fun updateChartWithSelectedEntry(entry: Entry) {
        val chartUIParams = getSharedChartUIParams(context)
        val (entriesBefore, entriesAfter) = currentEntries.partition { it.x <= entry.x }

        val entriesColor = currentEntries.getColorResForEntries()
        val datasetBefore = entriesBefore.createDataSet(entriesColor)
        val datasetAfter = entriesAfter.createDataSet(R.color.neutral_price_chart_line)

        chart.priceChartRenderer().apply {
            setDotPoint(entry)
            setDotColor(context.getColor(entriesColor))
        }

        chart.data = LineData(datasetBefore, datasetAfter)
        chart.invalidate()

        onSelectEntry(entriesBefore, isEntrySelected = true)
    }

    private fun updateChart() {
        val chartUIParams = getSharedChartUIParams(context)
        chart.priceChartRenderer().apply {
            setDotPoint(null)
            setDotColor(null)
        }

        val dataSet = currentEntries.createDataSet(currentEntries.getColorResForEntries())
        chart.data = LineData(dataSet)
        chart.invalidate()

        onSelectEntry(currentEntries, isEntrySelected = false)
    }

    private fun onSelectEntry(entries: List<Entry>, isEntrySelected: Boolean) {
        if (entries.isEmpty()) return

        callback.onSelectEntry(entries.first(), entries.last(), isEntrySelected)
    }

    private fun List<Entry>.createDataSet(colorRes: Int): LineDataSet {
        return LineDataSet(this, "").apply {
            color = context.getColor(colorRes)
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            lineWidth = chartUIParams.chartLineWidthDp
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

    private fun LineChart.getEntryByTouchPoint(event: MotionEvent): Entry? {
        val xTouch = chart.getTransformer(YAxis.AxisDependency.RIGHT)
            .getValuesByTouchPoint(event.x, event.y)
            .x

        val foundIndex = currentEntries.binarySearchFloor { it.x.compareTo(xTouch) }
        if (foundIndex >= 0 && foundIndex < currentEntries.size) {
            return currentEntries[foundIndex]
        } else {
            return null
        }
    }
}
