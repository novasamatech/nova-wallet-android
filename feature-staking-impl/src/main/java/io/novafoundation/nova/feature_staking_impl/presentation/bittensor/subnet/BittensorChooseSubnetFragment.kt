package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Bundle

import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorChooseSubnetBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries

class BittensorChooseSubnetFragment : BaseFragment<BittensorChooseSubnetViewModel, FragmentBittensorChooseSubnetBinding>() {

    companion object {
        private const val PAYLOAD_KEY = "payload"

        fun getBundle(payload: BittensorChooseSubnetPayload) = Bundle().apply { putParcelable(PAYLOAD_KEY, payload) }
    }

    private val imageLoader by lazy { FeatureUtils.getCommonApi(requireContext()).imageLoader() }

    private val adapter by lazy {
        BittensorSubnetListAdapter(imageLoader, viewModel::rootStaking, viewModel::select, viewModel::toggleFavorite)
    }

    private val shimmeringAdapter = CustomPlaceholderAdapter(R.layout.layout_bittensor_subnets_shimmering)

    private val rootAdapter by lazy {
        BittensorSubnetListAdapter(imageLoader, viewModel::rootStaking, viewModel::select, viewModel::toggleFavorite)
    }

    private var filterSheet: BittensorSubnetFiltersBottomSheet? = null

    private var filterDescription = ""

    override fun createBinding() = FragmentBittensorChooseSubnetBinding.inflate(layoutInflater)

    override fun inject() {
        val payload = argumentOrNull<BittensorChooseSubnetPayload>(PAYLOAD_KEY) ?: BittensorChooseSubnetPayload()
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorSubnetComponentFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun initViews(): Unit = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        subnets.adapter = ConcatAdapter(rootAdapter, shimmeringAdapter, adapter)
        toolbar.searchField.content.doAfterTextChanged { viewModel.search(it.toString()) }
        sorting.setDrawableEnd(R.drawable.ic_chevron_right_16, widthInDp = 16, tint = R.color.icon_accent)
        sorting.setOnClickListener { viewModel.sortingMixin.selectorClicked() }
        filters.setOnClickListener {
            filterSheet?.dismiss()
            filterSheet = BittensorSubnetFiltersBottomSheet(
                context = requireContext(),
                initialHideThinPools = viewModel.hideThinPools.value,
                resultLabel = viewModel::filterResult,
                description = filterDescription,
                onApply = { viewModel.hideThinPools.value = it }
            ).also { it.show() }
        }
    }

    override fun subscribe(viewModel: BittensorChooseSubnetViewModel) = with(binder) {
        observeBittensorRetries(viewModel.retryEvents)
        viewModel.sortingMixin.selectedOption.observe { sorting.text = it.display }
        viewModel.sortingMixin.chooseNewOption.awaitableActionLiveData.observeEvent { action ->
            BittensorSubnetSortBottomSheet(
                context = requireContext(),
                payload = action.payload,
                onClicked = { _, item -> action.onSuccess(item) },
                onCancel = action.onCancel
            ).show()
        }
        viewModel.hideThinPools.observe { filters.setImageTintRes(if (it) R.color.icon_accent else R.color.icon_secondary) }
        viewModel.filterDescription.observe { filterDescription = it }
        viewModel.subnetCount.observe { subnetCount.text = it }
        viewModel.list.observe { list ->
            rootAdapter.submitList(list.filterIsInstance<BittensorSubnetListItem.Root>())
            adapter.submitList(list.filterIsInstance<BittensorSubnetListItem.Subnet>())
        }
        viewModel.isEmpty.observe { empty.isVisible = it }
        viewModel.emptyMessage.observe { empty.text = it }
        viewModel.isLoading.observe(shimmeringAdapter::show)
    }

    override fun onDestroyView() {
        filterSheet?.dismiss()
        filterSheet = null
        super.onDestroyView()
    }
}
