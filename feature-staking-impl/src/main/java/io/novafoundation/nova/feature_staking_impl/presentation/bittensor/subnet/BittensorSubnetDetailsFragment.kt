package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorPositionAdapter
import coil.request.ImageRequest
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorSubnetDetailsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorSubnetIcon
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.displayName
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingValue
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartSelectionListener
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartsView
import kotlinx.coroutines.flow.combine

private const val PAYLOAD_KEY = "payload"

class BittensorSubnetDetailsFragment : BaseFragment<BittensorSubnetDetailsViewModel, FragmentBittensorSubnetDetailsBinding>() {

    companion object {

        fun getBundle(payload: BittensorSubnetDetailsPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    private val imageLoader by lazy {
        FeatureUtils.getCommonApi(requireContext())
            .imageLoader()
    }

    private val validatorAdapter by lazy { BittensorPositionAdapter(imageLoader) { viewModel.validatorClicked() } }

    private val validatorShimmer = CustomPlaceholderAdapter(R.layout.item_bittensor_subnet_shimmering)

    override fun createBinding() = FragmentBittensorSubnetDetailsBinding.inflate(layoutInflater)

    override fun inject() {
        val payload = argument<BittensorSubnetDetailsPayload>(PAYLOAD_KEY)
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorSubnetDetailsComponentFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        useSubnet.setOnClickListener { viewModel.continueClicked() }
        favorite.setOnClickListener { viewModel.toggleFavorite() }
        availableValidators.adapter = ConcatAdapter(validatorShimmer, validatorAdapter)
        chooseValidator.setOnClickListener { viewModel.validatorClicked() }
        tao.setOnClickListener { viewModel.showFiat(false) }
        fiat.setOnClickListener { viewModel.showFiat(true) }
        chart.onContentVisibilityChanged = { change.isVisible = it }
        chart.setPresentation(PriceChartsView.Presentation.PLOT_ONLY)
        chart.setPlotHeight(208)
        chart.onPeriodSelected = viewModel::selectPeriod
        chart.selectionListener = PriceChartSelectionListener { selection ->
            viewModel.selectChartPoint(selection)
            chartDate.isVisible = selection != null
            if (selection != null) {
                viewModel.chartDateFormatter.format(selection.point.timestamp, true, chartDate, selection.chart)
            } else {
                chartDate.text = null
            }
        }
        more.setOnClickListener {
            moreDetails.isVisible = !moreDetails.isVisible
            moreArrow.setImageResource(if (moreDetails.isVisible) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down)
        }
        website.setOnClickListener { viewModel.websiteClicked() }
        github.setOnClickListener { viewModel.githubClicked() }
    }

    override fun subscribe(viewModel: BittensorSubnetDetailsViewModel) = with(binder) {
        observeBittensorRetries(viewModel.retryEvents)
        observeBrowserEvents(viewModel)
        viewModel.website.observe { label ->
            label?.let { website.showValue(it) }
            website.isVisible = label != null
        }
        viewModel.github.observe { label ->
            label?.let { github.showValue(it) }
            github.isVisible = label != null
        }
        viewModel.selectedChartIndex.observe(chart::setSelectedPeriod)
        viewModel.nativeTokenSymbol.observe { tao.text = it }
        viewModel.subnet.observe { subnet ->
            val name = subnet.displayName(requireContext())
            toolbar.setTitle("\u200e$name")
            priceTitle.text = getString(R.string.price_chart_title, name)
            subnetNumber.showValue(subnet.id.value.toString())
            imageLoader.enqueue(
                ImageRequest.Builder(requireContext())
                    .data(subnet.logoUrl)
                    .size(28.dp(requireContext())).apply { bittensorSubnetIcon() }
                    .target {
                        val size = 28.dp(requireContext())
                        it.setBounds(
                            0,
                            0,
                            size,
                            size
                        )
                        toolbar.setTitleIcon(it)
                    }
                    .build()
            )
        }
        viewModel.price.observe { price.text = it }
        viewModel.currencyLabel.observe { fiat.text = it }
        viewModel.isFiatMode.observe {
            tao.isSelected = !it
            fiat.isSelected = it
        }
        combine(viewModel.charts, viewModel.priceChangeFormatter, ::Pair).observe { (charts, formatter) ->
            chart.onPriceChange = { from, to -> formatter.format(from, to, change) }
            chart.setCharts(charts)
        }
        viewModel.axisFormatter.observe(chart::setAxisLabelFormatter)
        viewModel.age.observe(age::showBittensorLoadingValue)
        viewModel.parameters.observe { bindSubnetParameters(it) }
        viewModel.validatorCount.observe(validatorCount::showBittensorLoadingValue)
        viewModel.isValidatorLoading.observe {
            validatorShimmer.show(it)
        }
        viewModel.availableValidators.observe(validatorAdapter::submitList)
        viewModel.canChooseValidator.observe { chooseValidator.isVisible = it }
        viewModel.canContinue.observe { useSubnet.isEnabled = it }
        viewModel.favorite.observe {
            favorite.setImageResource(if (it) R.drawable.ic_favorite_heart_filled else R.drawable.ic_favorite_heart_outline)
            favorite.contentDescription = getString(if (it) R.string.bittensor_remove_favorite else R.string.bittensor_add_favorite)
        }
    }
}
