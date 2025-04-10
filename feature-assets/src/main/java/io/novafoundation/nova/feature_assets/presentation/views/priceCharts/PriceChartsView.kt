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
import androidx.core.view.isGone
import com.github.mikephil.charting.data.Entry
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ViewPriceChartsBinding
import io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters.DateChartTextInjector
import io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters.PriceChangeTextInjector
import io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters.PriceTextInjector
import java.math.BigDecimal
import kotlin.math.roundToLong

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

    private val binder = ViewPriceChartsBinding.inflate(inflater(), this)

    private var charts: List<PriceChartModel> = emptyList()
    private var selectedChartIndex = 0

    private val horizontalScrollDetector = HorizontalScrollDetector(5.dpF)

    private val controller: ChartController

    private var priceTextInjector: PriceTextInjector? = null
    private var priceChangeTextInjector: PriceChangeTextInjector? = null
    private var dateTextInjector: DateChartTextInjector? = null

    init {
        controller = ChartController(binder.priceChart, this)
        setEmptyState()
    }

    fun setTitle(title: String) {
        binder.priceChartTitle.text = title
    }

    fun setCharts(charts: List<PriceChartModel>) {
        this.charts = charts

        binder.priceChartButtons.removeAllViews()
        charts.forEachIndexed { index, priceChartModel ->
            if (index > 0) {
                binder.priceChartButtons.addView(createSpace())
            }

            binder.priceChartButtons.addView(createButton(priceChartModel.buttonText, index))
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
        priceTextInjector?.format(selectedEntry.y, binder.priceChartCurrentPrice, isEntrySelected)
        priceChangeTextInjector?.format(startEntry.y, selectedEntry.y, binder.priceChartPriceChange)
        dateTextInjector?.format(selectedEntry.x.roundToLong(), isEntrySelected, binder.priceChartDate, charts[selectedChartIndex])
    }

    private fun setEmptyState() {
        showCharts(false)
    }

    private fun showCharts(show: Boolean) {
        binder.priceChart.setVisible(show, falseState = View.INVISIBLE)
        binder.priceChartDate.setVisible(show, falseState = View.INVISIBLE)
        binder.priceChartPriceChange.setVisible(show, falseState = View.INVISIBLE)
        binder.priceChartCurrentPrice.setVisible(show, falseState = View.INVISIBLE)
        binder.priceChartPriceChangeShimmering.isGone = show
        binder.priceChartCurrentPriceShimmering.isGone = show
        binder.priceChartShimmering.isGone = show
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

            showCharts(true)
        }
    }

    private fun getButtons(): List<TextView> {
        return binder.priceChartButtons.children
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
        val isTouchIntercepted = controller.isTouchIntercepted()
        if (isTouchIntercepted) {
            parent.requestDisallowInterceptTouchEvent(true)
        }

        return super.onInterceptTouchEvent(ev)
    }

    private fun PriceChartModel.Chart.asEntries() = priceChart.map {
        Entry(it.timestamp.toFloat(), it.price.toFloat())
    }
}
