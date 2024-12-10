package io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool

import android.os.Bundle
import androidx.core.view.isVisible

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.onLoaded
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.scrollToTopWhenItemsShuffled
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSelectPoolBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.PoolAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.PoolRvItem
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.SelectingPoolPayload
import javax.inject.Inject

class SelectPoolFragment : BaseFragment<SelectPoolViewModel, FragmentSelectPoolBinding>(), PoolAdapter.ItemHandler {

    companion object {

        const val PAYLOAD_KEY = "SelectCustomPoolFragment.Payload"

        fun getBundle(payload: SelectingPoolPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun createBinding() = FragmentSelectPoolBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    val adapter by lazy {
        PoolAdapter(imageLoader, this)
    }

    override fun initViews() {
        binder.selectPoolToolbar.applyStatusBarInsets()

        binder.selectPoolToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.selectPoolToolbar.addCustomAction(R.drawable.ic_search) {
            viewModel.searchClicked()
        }

        binder.selectPoolList.adapter = adapter
        binder.selectPoolList.setHasFixedSize(true)
        binder.selectPoolList.scrollToTopWhenItemsShuffled(viewLifecycleOwner)

        binder.selectPoolRecommendedAction.setOnClickListener { viewModel.selectRecommended() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .selectPoolComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: SelectPoolViewModel) {
        setupExternalActions(viewModel)

        viewModel.poolModelsFlow.observe { loadingState ->
            loadingState.onLoaded {
                adapter.submitList(it)
            }
            binder.selectPoolProgressBar.isVisible = loadingState.isLoading()
        }

        viewModel.selectedTitle.observe(binder.selectPoolCount::setText)

        viewModel.fillWithRecommendedEnabled.observe(binder.selectPoolRecommendedAction::setEnabled)
    }

    override fun poolInfoClicked(poolItem: PoolRvItem) {
        viewModel.poolInfoClicked(poolItem)
    }

    override fun poolClicked(poolItem: PoolRvItem) {
        viewModel.poolClicked(poolItem)
    }
}
