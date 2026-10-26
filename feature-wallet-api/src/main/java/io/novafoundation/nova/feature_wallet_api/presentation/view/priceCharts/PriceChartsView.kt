package io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.marginEnd
import androidx.core.view.updateLayoutParams
import com.facebook.shimmer.ShimmerFrameLayout
import com.github.mikephil.charting.data.Entry
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_wallet_api.databinding.LayoutPriceChartButtonBinding
import io.novafoundation.nova.feature_wallet_api.databinding.ViewPriceChartsBinding
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.ChartAxisFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.DateChartTextInjector
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.PriceChangeTextInjector
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.PriceTextInjector
import java.math.BigDecimal
import kotlin.math.roundToLong

sealed class PriceChartModel(val buttonText: String) {

    class Loading(buttonText: String) : PriceChartModel(buttonText)

    /**
     * There is nothing to draw for this period - either the price feed returned no points or the request has failed
     */
    class Empty(buttonText: String, val message: String) : PriceChartModel(buttonText)

    /**
     * [priceChart] is expected to be non-empty - a period without points should be mapped to [Empty] instead
     */
    class Chart(
        buttonText: String,
        val periodName: String,
        val supportTimeShowing: Boolean,
        val priceChart: List<Price>
    ) : PriceChartModel(buttonText) {

        class Price(val timestamp: Long, val price: BigDecimal)
    }
}

class PriceChartsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle), ChartController.Callback {

    private val binder = ViewPriceChartsBinding.inflate(inflater(), this)

    private val fullLayoutConstraints = ConstraintSet().apply { clone(this@PriceChartsView) }

    private var charts: List<PriceChartModel> = emptyList()

    private var selectedChartIndex = 0

    enum class Presentation { FULL, PLOT_ONLY }

    private var presentation = Presentation.FULL

    /** A plot-only presentation for screens that already own their amount header. */
    fun setPresentation(presentation: Presentation) {
        this.presentation = presentation
        controller.setFilledLine(presentation == Presentation.PLOT_ONLY)
        when (presentation) {
            Presentation.FULL -> showFullPresentation()
            Presentation.PLOT_ONLY -> showPlotOnlyPresentation()
        }
    }

    private fun showFullPresentation() {
        fullLayoutConstraints.applyTo(this)
        binder.priceChartTitle.isGone = false
        if (charts.isEmpty()) showLoadingState() else selectChart(selectedChartIndex)
    }

    private fun showPlotOnlyPresentation() {
        binder.priceChartTitle.isGone = true
        binder.priceChartCurrentPrice.isGone = true
        binder.priceChartPriceChange.isGone = true
        binder.priceChartDate.isGone = true
        binder.priceChartCurrentPriceShimmering.isGone = true
        binder.priceChartPriceChangeShimmering.isGone = true
        ConstraintSet().apply {
            clone(this@PriceChartsView)
            connect(binder.priceChart.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            applyTo(this@PriceChartsView)
        }
    }

    var onPeriodSelected: ((Int) -> Unit)? = null

    var onPriceChange: ((Float, Float) -> Unit)? = null

    var selectionListener: PriceChartSelectionListener? = null

    var onContentVisibilityChanged: ((Boolean) -> Unit)? = null

    fun setSelectedPeriod(index: Int) {
        selectedChartIndex = index
        if (index in charts.indices) selectChart(index)
    }

    fun setPlotHeight(heightDp: Int) {
        binder.priceChart.layoutParams = binder.priceChart.layoutParams.apply { height = heightDp.dpF.toInt() }
    }

    fun setReferencePrice(value: Float?, label: String) = controller.setReferencePrice(value, label)

    /** The width the loading skeleton reserves for axis labels, so it can be given back when the axis is hidden. */
    private val plotShimmeringAxisMargin = binder.priceChartPlotShimmering.marginEnd

    /**
     * Shows or hides the right-hand amount labels together with the dashed grid, in the chart and in its loading
     * skeleton. Hidden, the plot stretches across the full view width; a reference line keeps its own label.
     */
    fun setAxisVisible(visible: Boolean) {
        controller.setAxisVisible(visible)
        binder.priceChartShimmering.children.filterIsInstance<ShimmerFrameLayout>().forEach { it.isGone = !visible }
        binder.priceChartPlotShimmering.updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginEnd = if (visible) plotShimmeringAxisMargin else 0
        }
    }

    fun setAxisLabelFormatter(formatter: ChartAxisFormatter) = controller.setAxisLabelFormatter(formatter)

    private val controller: ChartController

    private var priceTextInjector: PriceTextInjector? = null

    private var priceChangeTextInjector: PriceChangeTextInjector? = null

    private var dateTextInjector: DateChartTextInjector? = null

    init {
        controller = ChartController(binder.priceChart, this)
        showLoadingState()
    }

    fun setTitle(title: String) {
        binder.priceChartTitle.text = title
    }

    fun setCharts(charts: List<PriceChartModel>) {
        selectionListener?.onSelectionChanged(null)
        val periodLabelsChanged = this.charts.map { it.buttonText } != charts.map { it.buttonText }
        this.charts = charts

        if (periodLabelsChanged) {
            updatePeriodButtons(charts)
        }

        if (charts.isEmpty()) {
            showLoadingState()
        } else {
            if (selectedChartIndex >= charts.size) {
                selectedChartIndex = 0
            }

            selectChart(selectedChartIndex)
        }
    }

    private fun updatePeriodButtons(charts: List<PriceChartModel>) {
        binder.priceChartButtons.removeAllViews()
        charts.forEachIndexed { index, model ->
            if (index > 0) binder.priceChartButtons.addView(createSpace())
            binder.priceChartButtons.addView(createButton(model.buttonText, index))
        }
    }

    fun setTextInjectors(
        priceTextInjector: PriceTextInjector,
        priceChangeTextInjector: PriceChangeTextInjector,
        dateTextInjector: DateChartTextInjector
    ) {
        this.priceTextInjector = priceTextInjector
        this.priceChangeTextInjector = priceChangeTextInjector
        this.dateTextInjector = dateTextInjector
    }

    override fun onSelectEntry(startEntry: Entry, selectedEntry: Entry, isEntrySelected: Boolean) {
        val chart = charts.getOrNull(selectedChartIndex)
        val point = selectedEntry.data as? PriceChartModel.Chart.Price
        val selection = PriceChartSelection.from(chart, point, isEntrySelected)
        selectionListener?.onSelectionChanged(selection)
        onPriceChange?.invoke(startEntry.y, selectedEntry.y)
        if (presentation == Presentation.FULL) {
            priceChangeTextInjector?.format(startEntry.y, selectedEntry.y, binder.priceChartPriceChange)
            priceTextInjector?.format(selectedEntry.y, binder.priceChartCurrentPrice, isEntrySelected)
            dateTextInjector?.format(selectedEntry.x.roundToLong(), isEntrySelected, binder.priceChartDate, charts[selectedChartIndex])
        }
    }

    private fun showLoadingState() {
        setChartContentVisible(false)
        binder.priceChartEmptyLabel.isGone = true
        setShimmeringVisible(true)
    }

    private fun showEmptyState(message: String) {
        setChartContentVisible(false)
        binder.priceChartEmptyLabel.text = message
        binder.priceChartEmptyLabel.isGone = false
        setShimmeringVisible(false)
    }

    private fun showChartState() {
        setChartContentVisible(true)
        binder.priceChartEmptyLabel.isGone = true
        setShimmeringVisible(false)
    }

    private fun setChartContentVisible(visible: Boolean) {
        onContentVisibilityChanged?.invoke(visible)
        binder.priceChart.setVisible(visible, falseState = View.INVISIBLE)
        if (presentation == Presentation.PLOT_ONLY) return
        binder.priceChartDate.setVisible(visible, falseState = View.INVISIBLE)
        binder.priceChartPriceChange.setVisible(visible, falseState = View.INVISIBLE)
        binder.priceChartCurrentPrice.setVisible(visible, falseState = View.INVISIBLE)
    }

    private fun setShimmeringVisible(visible: Boolean) {
        binder.priceChartPriceChangeShimmering.isGone = !visible || presentation == Presentation.PLOT_ONLY
        binder.priceChartCurrentPriceShimmering.isGone = !visible || presentation == Presentation.PLOT_ONLY
        binder.priceChartShimmering.isGone = !visible
    }

    private fun selectChart(index: Int) {
        selectionListener?.onSelectionChanged(null)
        selectedChartIndex = index

        getButtons().forEachIndexed { i, view ->
            view.isSelected = i == index
        }

        when (val currentChart = charts[index]) {
            is PriceChartModel.Loading -> showLoadingState()

            is PriceChartModel.Empty -> showEmptyState(currentChart.message)

            is PriceChartModel.Chart -> {
                controller.setEntries(currentChart.asEntries())

                showChartState()
            }
        }
    }

    private fun getButtons(): List<TextView> {
        return binder.priceChartButtons.children
            .toList().filterIsInstance<TextView>()
    }

    private fun createButton(text: String, index: Int): View {
        val buttonBinder = LayoutPriceChartButtonBinding.inflate(inflater())
        return buttonBinder.priceChartButtonText.apply {
            setText(text)
            setOnClickListener {
                selectChart(index)
                onPeriodSelected?.invoke(index)
            }
        }
    }

    private fun createSpace(): Space {
        val space = Space(context)
        space.layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        return space
    }

    /**
     * We should disallow intercept touch events by parents when we move horizontally to prevent scrolling in parent
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val isTouchIntercepted = controller.isTouchIntercepted()
        if (isTouchIntercepted) {
            parent.requestDisallowInterceptTouchEvent(true)
        }

        return super.onInterceptTouchEvent(ev)
    }

    private fun PriceChartModel.Chart.asEntries() = priceChart.map {
        Entry(it.timestamp.toFloat(), it.price.toFloat(), it)
    }

    override fun onDetachedFromWindow() {
        selectionListener?.onSelectionChanged(null)
        super.onDetachedFromWindow()
    }
}
