package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import coil.transform.CircleCropTransformation
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorSubnetDetailsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

private const val PAYLOAD_KEY = "payload"

class BittensorSubnetDetailsFragment : BaseFragment<BittensorSubnetDetailsViewModel, FragmentBittensorSubnetDetailsBinding>() {

    companion object {

        fun getBundle(payload: BittensorSubnetDetailsPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }
    private val imageLoader by lazy { FeatureUtils.getCommonApi(requireContext()).imageLoader() }
    override fun createBinding() = FragmentBittensorSubnetDetailsBinding.inflate(layoutInflater)
    override fun inject() {
        val payload = argument<BittensorSubnetDetailsPayload>(PAYLOAD_KEY)
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorSubnetDetailsComponentFactory().create(this, payload).inject(this)
    }
    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        validator.image.updateLayoutParams { width = 18.dp(requireContext()); height = 18.dp(requireContext()) }
        validator.setOnClickListener { viewModel.validatorClicked() }
        useSubnet.setOnClickListener { viewModel.continueClicked() }
        favorite.setOnClickListener { viewModel.toggleFavorite() }
        preset1.setOnClickListener { viewModel.selectPreset(BittensorStakeAmountPreset.ONE) }
        preset5.setOnClickListener { viewModel.selectPreset(BittensorStakeAmountPreset.FIVE) }
        preset10.setOnClickListener { viewModel.selectPreset(BittensorStakeAmountPreset.TEN) }
        presetMax.setOnClickListener { viewModel.selectPreset(BittensorStakeAmountPreset.MAX) }
        tao.setOnClickListener { viewModel.showFiat(false) }
        fiat.setOnClickListener { viewModel.showFiat(true) }
        chart.showPlotOnly(change)
        chart.setPlotHeight(208)
        chart.onPeriodSelected = viewModel::selectPeriod
        more.setOnClickListener { subnetNumber.isVisible = !subnetNumber.isVisible }
    }
    override fun subscribe(viewModel: BittensorSubnetDetailsViewModel) = with(binder) {
        viewModel.selectedChartIndex.observe(chart::setSelectedPeriod)
        viewModel.nativeTokenSymbol.observe { tao.text = it }
        viewModel.presetLabels.observe { labels ->
            preset1.text = labels.getValue(BittensorStakeAmountPreset.ONE)
            preset5.text = labels.getValue(BittensorStakeAmountPreset.FIVE)
            preset10.text = labels.getValue(BittensorStakeAmountPreset.TEN)
            presetMax.text = labels.getValue(BittensorStakeAmountPreset.MAX)
        }
        viewModel.subnet.observe { subnet ->
            val name = listOf(subnet.name, subnet.symbol).filter(String::isNotBlank).joinToString(" ")
            toolbar.setTitle("\u200e$name")
            priceTitle.text = getString(R.string.price_chart_title, name)
            subnetNumber.showValue(subnet.id.value.toString())
            imageLoader.enqueue(
                coil.request.ImageRequest.Builder(requireContext()).data(subnet.logoUrl)
                    .transformations(CircleCropTransformation()).size(28.dp(requireContext()))
                    .placeholder(R.drawable.ic_nova).fallback(R.drawable.ic_nova).error(R.drawable.ic_nova)
                    .target {
                        val size = 28.dp(requireContext())
                        it.setBounds(0, 0, size, size)
                        toolbar.setTitleIcon(it)
                    }.build()
            )
        }
        viewModel.price.observe { price.text = it }
        viewModel.currencyLabel.observe { fiat.text = it }
        viewModel.isFiatMode.observe {
            tao.isSelected = !it
            fiat.isSelected = it
        }
        viewModel.priceChange.observe { change.text = it }
        viewModel.charts.observe(chart::setCharts)
        viewModel.axisFormatter.observe(chart::setAxisLabelFormatter)
        viewModel.estimatedAlpha.observe { estimate.text = it }
        viewModel.pool.observe(pool::showValue)
        viewModel.validatorCount.observe(validatorCount::showValue)
        viewModel.selectedPreset.observe { selected ->
            preset1.isSelected = selected == BittensorStakeAmountPreset.ONE
            preset5.isSelected = selected == BittensorStakeAmountPreset.FIVE
            preset10.isSelected = selected == BittensorStakeAmountPreset.TEN
            presetMax.isSelected = selected == BittensorStakeAmountPreset.MAX
        }
        viewModel.selectedValidator.observe { validator.showValue(it?.validator?.name ?: getString(R.string.bittensor_select_validator)) }
        viewModel.validatorIcon.observe { validator.image.isVisible = it != null; validator.image.setImageDrawable(it) }
        viewModel.isLoading.observe { useSubnet.isEnabled = !it }
        viewModel.favorite.observe {
            favorite.setImageResource(if (it) R.drawable.ic_favorite_heart_filled else R.drawable.ic_favorite_heart_outline)
            favorite.contentDescription = getString(if (it) R.string.bittensor_remove_favorite else R.string.bittensor_add_favorite)
        }
    }
}
