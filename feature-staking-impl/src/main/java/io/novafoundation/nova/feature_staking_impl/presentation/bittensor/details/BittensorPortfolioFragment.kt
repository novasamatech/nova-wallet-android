package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import android.os.Bundle
import io.novafoundation.nova.analytics.StakingPortfolioSource
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.PrimaryButton
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorPortfolioBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartSelectionListener
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartsView

class BittensorPortfolioFragment : BaseFragment<BittensorDetailsViewModel, FragmentBittensorPortfolioBinding>() {

    companion object {
        const val ANALYTICS_SOURCE = "bittensor_portfolio_analytics_source"
        private const val ENTRY = "bittensor_portfolio_entry"

        fun sourceBundle(source: StakingPortfolioSource, entry: BittensorPortfolioEntry) = Bundle().apply {
            putString(ANALYTICS_SOURCE, source.name)
            putString(ENTRY, entry.name)
        }

        fun entryFrom(arguments: Bundle?): BittensorPortfolioEntry = BittensorPortfolioEntry.entries
            .firstOrNull { it.name == arguments?.getString(ENTRY) } ?: BittensorPortfolioEntry.PORTFOLIO
    }

    private val imageLoader by lazy {
        FeatureUtils.getCommonApi(requireContext())
            .imageLoader()
    }

    private val adapter by lazy { BittensorPositionAdapter(imageLoader, viewModel::positionClicked) }

    private var isChartContentVisible = false
        set(value) {
            field = value
            updateChartChange()
        }

    private var isChartAvailable = true
        set(value) {
            field = value
            updateChartChange()
        }

    override fun createBinding() = FragmentBittensorPortfolioBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorDetailsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        positions.adapter = adapter
        addPosition.setOnClickListener { viewModel.addPosition() }
        priceChart.onContentVisibilityChanged = { isChartContentVisible = it }
        priceChart.setPresentation(PriceChartsView.Presentation.PLOT_ONLY)
        priceChart.setTitle(getString(R.string.staking_validator_total_stake))
        priceChart.setPlotHeight(156)
        priceChart.onPeriodSelected = viewModel::selectChartPeriod
        priceChart.selectionListener = PriceChartSelectionListener { selection ->
            viewModel.selectChartPoint(selection)
            chartDate.isVisible = selection != null
            if (selection != null) {
                viewModel.chartDateFormatter.format(selection.point.timestamp, true, chartDate, selection.chart)
            } else {
                chartDate.text = null
            }
        }
    }

    override fun subscribe(viewModel: BittensorDetailsViewModel) = with(binder) {
        viewModel.entryResolution.observe { resolution ->
            if (resolution != null) {
                arguments?.remove(ENTRY)
                viewModel.completeEntry(resolution)
            }
        }
        observeBittensorRetries(viewModel.retryEvents)
        viewModel.selectedChartIndex.observe(priceChart::setSelectedPeriod)
        viewModel.chartChangeFormatter.observe { it.format(chartChange) }
        viewModel.priceChartModels.observe(priceChart::setCharts)
        viewModel.chartAxisFormatter.observe(priceChart::setAxisLabelFormatter)
        viewModel.isLoading.observe {
            loading.isVisible = it
            totalAmount.isVisible = !it
            totalFiat.isVisible = !it
        }
        viewModel.canAddStake.observe { addPosition.isEnabled = it }
        viewModel.totalStake.observe {
            totalAmount.text = it.token
            totalFiat.text = it.fiat
        }
        viewModel.positions.observe(adapter::submitList)
        viewModel.empty.observe {
            empty.isVisible = it
            // iOS: the only action of an empty portfolio is primary, next to positions it is secondary.
            addPosition.setAppearance(if (it) PrimaryButton.Appearance.PRIMARY else PrimaryButton.Appearance.SECONDARY)
        }
        viewModel.rates.observe { rates ->
            ratesUnavailable.isVisible = rates.showBanner
            priceChart.isVisible = rates.showChart
            chartDisclaimer.isVisible = rates.showChart
            if (!rates.showChart) chartDate.isVisible = false
            isChartAvailable = rates.showChart
        }
        viewModel.warning.observe {
            warning.text = it
            warning.isVisible = it != null
        }
    }

    private fun updateChartChange() {
        binder.chartChange.isVisible = isChartContentVisible && isChartAvailable
    }
}
