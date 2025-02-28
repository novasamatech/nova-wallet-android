package io.novafoundation.nova.feature_assets.presentation.views.priceCharts

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.github.mikephil.charting.data.Entry
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters.DateChartTextInjector
import io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters.PriceChangeTextInjector
import io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters.PriceTextInjector
import java.math.BigDecimal
import kotlinx.android.synthetic.main.view_price_charts.view.priceChart
import kotlinx.android.synthetic.main.view_price_charts.view.priceChartButtons
import kotlinx.android.synthetic.main.view_price_charts.view.priceChartCurrentPrice
import kotlinx.android.synthetic.main.view_price_charts.view.priceChartCurrentPriceShimmering
import kotlinx.android.synthetic.main.view_price_charts.view.priceChartDate
import kotlinx.android.synthetic.main.view_price_charts.view.priceChartPriceChange
import kotlinx.android.synthetic.main.view_price_charts.view.priceChartPriceChangeShimmering
import kotlinx.android.synthetic.main.view_price_charts.view.priceChartTitle
import kotlin.math.roundToLong
import kotlin.math.sin

sealed class PriceChartModel(val buttonText: String) {

    class Loading(buttonText: String) : PriceChartModel(buttonText)

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

    private var charts: List<PriceChartModel> = emptyList()
    private var selectedChartIndex = 0

    private val horizontalScrollDetector = HorizontalScrollDetector(5.dpF)

    private val controller: ChartController

    private var priceTextInjector: PriceTextInjector? = null
    private var priceChangeTextInjector: PriceChangeTextInjector? = null
    private var dateTextInjector: DateChartTextInjector? = null

    init {
        View.inflate(context, R.layout.view_price_charts, this)
        controller = ChartController(priceChart, this)
        setEmptyState()
    }

    fun setTitle(title: String) {
        priceChartTitle.text = title
    }

    fun setCharts(charts: List<PriceChartModel>) {
        this.charts = charts

        priceChartButtons.removeAllViews()
        charts.forEachIndexed { index, priceChartModel ->
            if (index > 0) {
                priceChartButtons.addView(createSpace())
            }

            priceChartButtons.addView(createButton(priceChartModel.buttonText, index))
        }

        if (charts.isEmpty()) {
            setEmptyState()
        } else {
            if (selectedChartIndex >= charts.size) {
                selectedChartIndex = 0
            }

            selectChart(selectedChartIndex)
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
        priceTextInjector?.format(selectedEntry.y, priceChartCurrentPrice)
        priceChangeTextInjector?.format(startEntry.y, selectedEntry.y, priceChartPriceChange)
        dateTextInjector?.format(selectedEntry.x.roundToLong(), isEntrySelected, priceChartDate, charts[selectedChartIndex])
    }

    private fun setEmptyState() {
        val periods = 2
        val entriesCount = 100
        val entries = List(entriesCount) {
            val sinY = sin(periods * Math.PI * 2 * it / entriesCount)
            Entry(it.toFloat(), sinY.toFloat())
        }

        controller.setEntries(entries)

        showLoadingState(true)
    }

    private fun showLoadingState(show: Boolean) {
        priceChartDate.isInvisible = show
        priceChartPriceChange.isInvisible = show
        priceChartCurrentPrice.isInvisible = show
        priceChartPriceChangeShimmering.isVisible = show
        priceChartCurrentPriceShimmering.isVisible = show

        controller.useNeutralColor(show)
        controller.showYAxis(!show)
    }

    private fun selectChart(index: Int) {
        selectedChartIndex = index

        getButtons().forEachIndexed { i, view ->
            view.isSelected = i == index
        }

        val currentChart = charts[index]
        if (currentChart is PriceChartModel.Loading) {
            setEmptyState()
            return
        } else if (currentChart is PriceChartModel.Chart) {
            controller.setEntries(currentChart.asEntries())

            showLoadingState(false)
        }
    }

    private fun getButtons(): List<TextView> {
        return priceChartButtons.children
            .toList()
            .filterIsInstance<TextView>()
    }

    private fun createButton(text: String, index: Int): View {
        val button = View.inflate(context, R.layout.layout_price_chart_button, null) as TextView
        button.text = text
        button.setOnClickListener { selectChart(index) }
        return button
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
        val isHorizontalScroll = horizontalScrollDetector.isHorizontalScroll(ev)
        if (isHorizontalScroll) {
            parent.requestDisallowInterceptTouchEvent(true)
        }

        return super.onInterceptTouchEvent(ev)
    }

    private fun PriceChartModel.Chart.asEntries() = priceChart.map {
        Entry(it.timestamp.toFloat(), it.price.toFloat())
    }
}
