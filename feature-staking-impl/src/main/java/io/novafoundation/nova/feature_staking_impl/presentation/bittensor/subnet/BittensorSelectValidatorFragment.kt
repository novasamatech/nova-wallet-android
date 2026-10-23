package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Bundle
import android.widget.ImageView
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.dialog.dialog
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

    private var filterAction: ImageView? = null

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
        onBackPressed { viewModel.backPressed() }
        binder.list.itemAnimator = null
        binder.list.adapter = adapter
        binder.confirm.setOnClickListener { viewModel.confirmClicked() }
        binder.removeFilters.setOnClickListener { viewModel.removeFilters() }
        filterAction = binder.toolbar.addCustomAction(R.drawable.ic_filter) { viewModel.filtersClicked() }
            .apply { contentDescription = getString(R.string.common_filters) }
        binder.toolbar.addCustomAction(R.drawable.ic_search) { viewModel.searchClicked() }
            .contentDescription = getString(R.string.common_search)
        binder.searchField.content.doAfterTextChanged { viewModel.search(it.toString()) }
        binder.listFailure.setActionText(getString(R.string.common_try_again))
        binder.listFailure.setOnLinkClickedListener { viewModel.retryList() }
    }

    override fun subscribe(viewModel: BittensorSelectValidatorViewModel) = with(binder) {
        observeBittensorRetries(viewModel.retryEvents)
        viewModel.filters.observe { filterAction?.setImageTintRes(if (it.hideUnusualApy) R.color.icon_accent else R.color.icon_primary) }
        viewModel.searchVisible.observe { visible ->
            if (searchContainer.isVisible == visible) return@observe
            searchContainer.isVisible = visible
            if (!visible) {
                searchField.content.hideSoftKeyboard()
                searchField.content.text.clear()
            }
        }
        viewModel.searchFocusEvents.observe {
            searchField.content.requestFocus()
            searchField.content.showSoftKeyboard()
        }
        viewModel.listFailure.observe { failure ->
            listFailure.isVisible = failure != null
            listHeader.isVisible = failure == null
            failure?.let {
                listFailure.setMessage(it.title)
                listFailure.setSubMessage(it.message)
            }
        }
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
        viewModel.inactiveValidatorEvents.observe {
            dialog(requireContext()) {
                setTitle(R.string.bittensor_inactive_validator_title)
                setMessage(R.string.bittensor_inactive_validator_message)
                setPositiveButton(R.string.common_close, null)
            }
        }
        viewModel.emptyMessage.observe { empty.text = it }
        viewModel.canRemoveFilters.observe {
            removeFilters.isVisible = it
            emptyIcon.isVisible = it
        }
    }

    override fun onDestroyView() {
        filterAction = null
        binder.list.adapter = null
        super.onDestroyView()
    }
}
