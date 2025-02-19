package io.novafoundation.nova.feature_assets.presentation.views.priceCharts

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import kotlinx.android.synthetic.main.layout_price_chart_button.view.priceChartButtonText
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

sealed class PriceChartModel(val name: String) {

    class Loading(name: String) : PriceChartModel(name)

    class Chart(name: String, val priceChart: List<Price>) : PriceChartModel(name) {

        class Price(val timestamp: Long, val price: BigDecimal)
    }
}

class PriceChartsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle), ChartController.Callback {

    private var charts: List<PriceChartModel> = emptyList()

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
                priceChartButtons.addView(getSpace())
            }

            priceChartButtons.addView(getButton(priceChartModel.name, index))
        }

        if (charts.isEmpty()) {
            setEmptyState()
        } else {
            selectChart(0)
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

    override fun onSelectEntry(startEntry: Entry, selectedEntry: Entry, isLastEntry: Boolean) {
        priceTextInjector?.format(selectedEntry.y, priceChartCurrentPrice)
        priceChangeTextInjector?.format(startEntry.y, selectedEntry.y, priceChartPriceChange)
        dateTextInjector?.format(selectedEntry.x.roundToLong(), isLastEntry, priceChartDate)
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

    private fun getButton(text: String, index: Int): View {
        val button = View.inflate(context, R.layout.layout_price_chart_button, null) as TextView
        button.text = text
        button.setOnClickListener { selectChart(index) }
        return button
    }

    private fun getSpace(): Space {
        val space = Space(context)
        space.layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        return space
    }

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
