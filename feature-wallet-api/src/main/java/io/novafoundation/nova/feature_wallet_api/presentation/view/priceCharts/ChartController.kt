package io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import io.novafoundation.nova.common.utils.binarySearchFloor
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.vibrate
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.ChartAxisFormatter

class ChartController(private val chart: LineChart, private val callback: Callback) : View.OnTouchListener {

    interface Callback {
        fun onSelectEntry(startEntry: Entry, selectedEntry: Entry, isEntrySelected: Boolean)
    }

    private val context = chart.context

    private val chartUIParams = ChartUIParams.default(context)

    private val longClickDetector = LongPressDetector(cancelDistance = 5.dpF(context), timeout = 200, ::onChartsLongClick)

    private var currentEntries: List<Entry> = emptyList()

    private var useNeutralColor = false

    private var filledLine = false

    fun setFilledLine(enabled: Boolean) {
        filledLine = enabled
        chart.axisRight.apply {
            textSize = if (enabled) 12f else 9f
            typeface = if (enabled) Typeface.DEFAULT else Typeface.MONOSPACE
            setLabelCount(if (enabled) 3 else chartUIParams.gridLines, true)
        }
        updateChart()
    }

    init {
        setupChartUI()
    }

    fun showYAxis(show: Boolean) {
        chart.axisRight.setDrawLabels(show)
        chart.invalidate()
    }

    fun setReferencePrice(value: Float?, label: String) {
        chart.axisRight.removeAllLimitLines()
        if (value != null && value > 0 && !value.isInfinite() && !value.isNaN()) {
            val line = LimitLine(value, label)
            line.lineWidth = 1f
            line.lineColor = context.getColor(R.color.text_secondary)
            line.textColor = context.getColor(R.color.text_secondary)
            line.textSize = 11f
            line.enableDashedLine(4.dpF(context), 4.dpF(context), 0f)
            line.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            chart.axisRight.addLimitLine(line)
        }
        chart.invalidate()
    }

    fun setAxisLabelFormatter(formatter: ChartAxisFormatter) {
        chart.axisRight.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = formatter.format(value)
        }
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    fun useNeutralColor(useNeutral: Boolean) {
        this.useNeutralColor = useNeutral
        updateChart()
    }

    fun setEntries(entries: List<Entry>) {
        currentEntries = entries
        keepAxisAboveZero(entries)
        updateChart()
    }

    /**
     * The axis pads below the lowest value by [YAxis.getSpaceBottom] percent of the range, which for a series that
     * starts at zero (a portfolio before its first stake) labels the bottom grid line with a negative amount.
     */
    private fun keepAxisAboveZero(entries: List<Entry>) {
        chart.axisRight.resetAxisMinimum()
        val min = entries.minOfOrNull { it.y } ?: return
        val max = entries.maxOf { it.y }
        val paddedMin = min - (max - min) * chart.axisRight.spaceBottom / 100f
        if (min >= 0f && paddedMin < 0f) {
            chart.axisRight.axisMinimum = 0f
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupChartUI() {
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

        chart.setOnTouchListener(this)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        longClickDetector.onTouch(v, event)

        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                updateChart()
            }
        }

        handleChartsTouch(event)

        return true
    }

    fun onChartsLongClick(event: MotionEvent) {
        handleChartsTouch(event)

        context.vibrate(50) // Add very short vibration on long click
    }

    fun isTouchIntercepted(): Boolean {
        return longClickDetector.isLongClickDetected
    }

    private fun handleChartsTouch(event: MotionEvent) {
        if (longClickDetector.isLongClickDetected) {
            val entry = chart.getEntryByTouchPoint(event)
            if (entry != null) {
                updateChartWithSelectedEntry(entry)
            }
        }
    }

    private fun updateChartWithSelectedEntry(entry: Entry) {
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
            mode = if (filledLine) LineDataSet.Mode.LINEAR else LineDataSet.Mode.CUBIC_BEZIER
            lineWidth = if (filledLine) 2f else chartUIParams.chartLineWidthDp
            setDrawFilled(filledLine)
            fillColor = color
            fillAlpha = 32
        }
    }

    private fun LineChart.priceChartRenderer() = renderer as PriceChartRenderer

    private fun List<Entry>.getColorResForEntries(): Int {
        if (useNeutralColor) return R.color.neutral_price_chart_line

        return if (isBullish()) R.color.positive_price_chart_line else R.color.negative_price_chart_line
    }

    private fun List<Entry>.isBullish(): Boolean {
        val firstEntry = firstOrNull() ?: return true

        return last().y >= firstEntry.y
    }

    private fun LineChart.getEntryByTouchPoint(event: MotionEvent): Entry? {
        val xTouch = chart.getTransformer(YAxis.AxisDependency.RIGHT)
            .getValuesByTouchPoint(event.x, event.y).x

        val foundIndex = currentEntries.binarySearchFloor { it.x.compareTo(xTouch) }
        if (foundIndex >= 0 && foundIndex < currentEntries.size) {
            return currentEntries[foundIndex]
        } else {
            return null
        }
    }
}
