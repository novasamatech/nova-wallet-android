package io.novafoundation.nova.feature_staking_impl.presentation.pools.searchPool

import android.os.Bundle
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.scrollToTopWhenItemsShuffled
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSearchPoolBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.PoolAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.PoolRvItem
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.SelectingPoolPayload
import javax.inject.Inject

class SearchPoolFragment : BaseFragment<SearchPoolViewModel, FragmentSearchPoolBinding>(), PoolAdapter.ItemHandler {

    companion object {

        const val PAYLOAD_KEY = "SearchPoolViewModel.Payload"

        fun getBundle(payload: SelectingPoolPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun createBinding() = FragmentSearchPoolBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    val adapter by lazy {
        PoolAdapter(imageLoader, this)
    }

    override fun initViews() {
        binder.searchPoolToolbar.applyStatusBarInsets()

        binder.searchPoolToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.searchPoolList.adapter = adapter
        binder.searchPoolList.setHasFixedSize(true)
        binder.searchPoolList.scrollToTopWhenItemsShuffled(viewLifecycleOwner)

        binder.searchPoolToolbar.searchField.requestFocus()
        binder.searchPoolToolbar.searchField.content.showSoftKeyboard()
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .searchPoolComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: SearchPoolViewModel) {
        setupExternalActions(viewModel)
        observeValidations(viewModel)

        binder.searchPoolToolbar.searchField.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.poolModelsFlow.observe {
            binder.searchPoolListHeader.isInvisible = it.isEmpty()
            adapter.submitList(it)
        }

        viewModel.placeholderFlow.observe { placeholder ->
            binder.searchPoolPlaceholder.isVisible = placeholder != null
            placeholder?.let { binder.searchPoolPlaceholder.setModel(placeholder) }
        }

        viewModel.selectedTitle.observe(binder.searchPoolCount::setText)
    }

    override fun poolInfoClicked(poolItem: PoolRvItem) {
        viewModel.poolInfoClicked(poolItem)
    }

    override fun poolClicked(poolItem: PoolRvItem) {
        viewModel.poolClicked(poolItem)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binder.searchPoolToolbar.searchField.hideSoftKeyboard()
    }
}
