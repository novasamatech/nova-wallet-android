package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorPortfolioBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.BittensorStakeResultBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi

class BittensorPortfolioFragment : BaseFragment<BittensorDetailsViewModel, FragmentBittensorPortfolioBinding>() {

    private var recoverySheet: BittensorStakeResultBottomSheet? = null

    private val imageLoader by lazy { FeatureUtils.getCommonApi(requireContext()).imageLoader() }
    private val adapter by lazy { BittensorPositionAdapter(imageLoader, viewModel::positionClicked) }

    override fun createBinding() = FragmentBittensorPortfolioBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorDetailsComponentFactory().create(this).inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        positions.adapter = adapter
        addPosition.setOnClickListener { viewModel.addPosition() }
        retry.setOnClickListener { viewModel.retry() }
        priceChart.showPlotOnly(chartChange)
        priceChart.setPlotHeight(156)
        priceChart.onPeriodSelected = viewModel::selectChartPeriod
        chartRetry.setOnClickListener { viewModel.retryChart() }
    }

    override fun subscribe(viewModel: BittensorDetailsViewModel) = with(binder) {
        viewModel.selectedChartIndex.observe(priceChart::setSelectedPeriod)
        viewModel.priceChartTitle.observe(priceChart::setTitle)
        viewModel.priceChartFormatters.observe { priceChart.setTextInjectors(it.price, it.priceChange, it.date) }
        viewModel.priceChartModels.observe(priceChart::setCharts)
        viewModel.chartAxisFormatter.observe(priceChart::setAxisLabelFormatter)
        viewModel.chartFailed.observe { chartRetry.isVisible = it }
        viewModel.isLoading.observe { loading.isVisible = it }
        viewModel.canAddStake.observe { addPosition.isEnabled = it }
        viewModel.totalStake.observe {
            totalAmount.text = it.token
            totalFiat.text = it.fiat
        }
        viewModel.positions.observe(adapter::submitList)
        viewModel.empty.observe { empty.isVisible = it }
        viewModel.recoveryResult.observe { recovered ->
            if (recovered == null) dismissRecoverySheet() else showRecoveryResult(recovered)
        }
        viewModel.warning.observe {
            warning.text = it
            warning.isVisible = it != null
            retry.isVisible = it != null
        }
    }
    private fun showRecoveryResult(result: BittensorStakeResultUi.Recovered) {
        val sheet = recoverySheet ?: BittensorStakeResultBottomSheet(
            requireContext(),
            onClose = { recoverySheet?.dismiss() },
            onTryAgain = {},
            onDiscoverSubnets = { recoverySheet?.dismiss() },
            onViewPosition = { recoverySheet?.dismiss() }
        ).also {
            recoverySheet = it
            it.show()
        }
        sheet.setState(result)
        sheet.setOnDismissListener {
            recoverySheet = null
            viewModel.recoveryResultDismissed(result.metaId, result.transactionHash)
        }
    }

    private fun dismissRecoverySheet() {
        recoverySheet?.setOnDismissListener(null)
        recoverySheet?.dismiss()
        recoverySheet = null
    }

    override fun onDestroyView() {
        dismissRecoverySheet()
        super.onDestroyView()
    }
}
