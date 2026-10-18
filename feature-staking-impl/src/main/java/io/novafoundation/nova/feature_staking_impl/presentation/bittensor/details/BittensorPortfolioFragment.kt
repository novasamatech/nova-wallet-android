package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorPortfolioBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartsView

class BittensorPortfolioFragment : BaseFragment<BittensorDetailsViewModel, FragmentBittensorPortfolioBinding>() {

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
        priceChart.setPlotHeight(156)
        priceChart.onPeriodSelected = viewModel::selectChartPeriod
    }

    override fun subscribe(viewModel: BittensorDetailsViewModel) = with(binder) {
        observeBittensorRetries(viewModel.retryEvents)
        viewModel.selectedChartIndex.observe(priceChart::setSelectedPeriod)
        viewModel.priceChartDisclaimer.observe { chartDisclaimer.text = it }
        viewModel.priceChartTitle.observe(priceChart::setTitle)
        viewModel.priceChartFormatters.observe { formatters ->
            priceChart.onPriceChange = { from, to -> formatters.priceChange.format(from, to, chartChange) }
        }
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
