package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Bundle
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import androidx.core.widget.doAfterTextChanged
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorSelectValidatorBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries

private const val PAYLOAD_KEY = "payload"

class BittensorSelectValidatorFragment : BaseFragment<BittensorSelectValidatorViewModel, FragmentBittensorSelectValidatorBinding>() {

    companion object {

        fun getBundle(payload: BittensorSelectValidatorCommunicator.Request) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    private val adapter by lazy { BittensorValidatorAdapter(viewModel::select, viewModel::infoClicked) }

    private var filtersSheet: BittensorValidatorFiltersBottomSheet? = null

    override fun createBinding() = FragmentBittensorSelectValidatorBinding.inflate(layoutInflater)

    override fun inject() {
        val payload = argument<BittensorSelectValidatorCommunicator.Request>(PAYLOAD_KEY)
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorSelectValidatorComponentFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun initViews() {
        binder.toolbar.setHomeButtonListener { viewModel.back() }
        binder.toolbar.toolbar.setHomeButtonIcon(R.drawable.ic_arrow_back)
        onBackPressed { viewModel.back() }
        binder.list.itemAnimator = null
        binder.list.adapter = adapter
        binder.confirm.setOnClickListener { viewModel.confirmClicked() }
        binder.removeFilters.setOnClickListener { viewModel.removeFilters() }
        binder.toolbar.toolbar.setRightIconRes(R.drawable.ic_filter)
        binder.toolbar.toolbar.setRightActionClickListener { showFilters() }
        binder.toolbar.searchField.content.doAfterTextChanged { viewModel.search(it.toString()) }
    }

    override fun subscribe(viewModel: BittensorSelectValidatorViewModel) = with(binder) {
        observeBittensorRetries(viewModel.retryEvents)
        viewModel.filters.observe { toolbar.toolbar.setRightActionTint(if (it.hideUnusualApy) R.color.icon_accent else R.color.icon_primary) }
        setupExternalActions(viewModel)
        viewModel.header.observe {
            count.text = it.count
            sortLabel.text = it.sorting
        }
        viewModel.rows.observe(adapter::submitList)
        viewModel.selectedAddress.observe(adapter::select)
        viewModel.buttonState.observe(confirm::setState)
        viewModel.buttonLabel.observe { confirm.text = it }
        viewModel.isLoading.observe {
            progress.isVisible = it
        }
        viewModel.isEmpty.observe { emptyState.isVisible = it }
        viewModel.emptyMessage.observe { empty.text = it }
        viewModel.canRemoveFilters.observe {
            removeFilters.isVisible = it
            emptyIcon.isVisible = it
        }
    }

    override fun onDestroyView() {
        filtersSheet?.dismiss()
        filtersSheet = null
        binder.list.adapter = null
        super.onDestroyView()
    }

    private fun showFilters() {
        filtersSheet?.dismiss()
        filtersSheet = BittensorValidatorFiltersBottomSheet(requireContext(), viewModel.filters.value, viewModel::applyFilters)
            .also { it.show() }
    }
}
