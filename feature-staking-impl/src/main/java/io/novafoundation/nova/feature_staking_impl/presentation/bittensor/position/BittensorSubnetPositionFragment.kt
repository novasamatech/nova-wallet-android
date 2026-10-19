package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position

import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.utils.setTextColorRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import coil.transform.RoundedCornersTransformation
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingAmount
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.databinding.BottomSheeetFixedListBinding
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.utils.EventObserver
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorSubnetPositionBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorPositionAdapter
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartSelectionListener
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartsView

class BittensorSubnetPositionFragment : BaseFragment<BittensorSubnetPositionViewModel, FragmentBittensorSubnetPositionBinding>() {

    companion object {

        private const val PAYLOAD = "BittensorSubnetPositionFragment.Payload"

        fun getBundle(payload: BittensorSubnetPositionPayload) = bundleOf(PAYLOAD to payload)
    }

    private val imageLoader by lazy {
        FeatureUtils.getCommonApi(requireContext())
            .imageLoader()
    }

    private val adapter by lazy { BittensorPositionAdapter(imageLoader, viewModel::validatorClicked) }

    private var choiceSheet: FixedListBottomSheet<BottomSheeetFixedListBinding>? = null

    override fun createBinding() = FragmentBittensorSubnetPositionBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorSubnetPositionComponentFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        sell.setOnClickListener { viewModel.sellClicked() }
        buy.setOnClickListener { viewModel.buyClicked() }
        validators.adapter = adapter
        chart.setPresentation(PriceChartsView.Presentation.PLOT_ONLY)
        chart.onContentVisibilityChanged = { change.isVisible = it }
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
    }

    override fun subscribe(viewModel: BittensorSubnetPositionViewModel) = with(binder) {
        setupExternalActions(viewModel)
        observeBittensorRetries(viewModel.retryEvents)
        viewModel.subnet.observe { subnet ->
            val name = subnet?.name?.ifBlank { subnet.symbol } ?: getString(R.string.bittensor_exit_position)
            toolbar.setTitle(name)
            subnetCaption.text = name
            subnetIcon.setIcon(
                subnet?.logoUrl?.asUrlIcon() ?: AssetIconProvider.fallbackIcon,
                imageLoader
            ) {
                transformations(
                    RoundedCornersTransformation(
                        16.dp(requireContext()).toFloat()
                    )
                )
            }
            priceTitle.text = getString(R.string.price_chart_title, name)
        }
        viewModel.averagePrice.observe(averagePrice::showBittensorLoadingAmount)
        viewModel.earned.observe { state ->
            earned.showBittensorLoadingAmount(state.map { it.amount })
            earned.valuePrimary.setTextColorRes(state.dataOrNull?.colorRes ?: R.color.text_secondary)
        }
        viewModel.price.observe { price.text = it }
        viewModel.priceChangeFormatter.observe { formatter ->
            chart.onPriceChange = { from, to -> formatter.format(from, to, change) }
        }
        viewModel.averageChartPrice.observe { value ->
            chart.setReferencePrice(value?.toFloat(), getString(R.string.bittensor_exit_avg_buy_price))
        }
        viewModel.holding.observe { holding.text = it }
        viewModel.hasStake.observe { activeStatus.isVisible = it }
        viewModel.worth.observe(worth::showBittensorLoadingAmount)
        viewModel.validators.observe(adapter::submitList)
        viewModel.canAct.observe {
            buy.isEnabled = it
        }
        viewModel.canSell.observe { sell.isEnabled = it }
        viewModel.withdrawalLoading.observe { withdrawalLoading.isVisible = it }
        viewModel.withdrawalHint.observe {
            withdrawalHint.text = it
            withdrawalHint.isVisible = it != null
        }
        viewModel.axisFormatter.observe(chart::setAxisLabelFormatter)
        viewModel.charts.observe(chart::setCharts)
        viewModel.selectedChartIndex.observe(chart::setSelectedPeriod)
        viewModel.positionError.observe {
            positionError.text = it
            positionError.isVisible = it != null
        }
        viewModel.choices.observe(
            viewLifecycleOwner,
            EventObserver { (action, choices) ->
                choiceSheet?.dismiss()
                val configuration = FixedListBottomSheet.ViewConfiguration.default(requireContext())
                val sheet = object : FixedListBottomSheet<BottomSheeetFixedListBinding>(requireContext(), configuration) {}
                choiceSheet = sheet
                sheet.setTitle(R.string.bittensor_exit_select_position)
                choices.forEach { payload ->
                    val row = TableCellView.createTableCellView(requireContext())
                    row.title.text = payload.hotkeyAddress
                    row.setOnClickListener {
                        sheet.dismiss()
                        viewModel.selectPosition(action, payload)
                    }
                    sheet.addItem(row)
                }
                sheet.show()
            }
        )
    }

    override fun onDestroyView() {
        choiceSheet?.dismiss()
        choiceSheet = null
        super.onDestroyView()
    }
}
