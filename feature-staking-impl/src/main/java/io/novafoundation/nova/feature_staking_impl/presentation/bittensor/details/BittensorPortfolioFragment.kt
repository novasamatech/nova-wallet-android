package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import android.os.Bundle
import io.novafoundation.nova.analytics.StakingPortfolioSource
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorPortfolioBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.common.view.bottomSheet.action.observeActionBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartSelectionListener
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartsView

class BittensorPortfolioFragment : BaseFragment<BittensorDetailsViewModel, FragmentBittensorPortfolioBinding>() {

    companion object {
        const val ANALYTICS_SOURCE = "bittensor_portfolio_analytics_source"

        fun sourceBundle(source: StakingPortfolioSource) = Bundle().apply {
            putString(ANALYTICS_SOURCE, source.name)
        }
    }

    private val imageLoader by lazy {
        FeatureUtils.getCommonApi(requireContext())
            .imageLoader()
    }

    private val adapter by lazy { BittensorPositionAdapter(imageLoader, viewModel::positionClicked) }

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
        priceChart.onContentVisibilityChanged = { chartChange.isVisible = it }
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
        observeBittensorRetries(viewModel.retryEvents)
        observeActionBottomSheet(viewModel.walletGate)
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
        viewModel.empty.observe { empty.isVisible = it }
        viewModel.warning.observe {
            warning.text = it
            warning.isVisible = it != null
        }
    }
}
