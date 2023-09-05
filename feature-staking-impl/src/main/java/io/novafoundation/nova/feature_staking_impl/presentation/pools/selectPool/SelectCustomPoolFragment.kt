package io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.scrollToTopWhenItemsShuffled
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.PoolAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.PoolRvItem
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_select_custom_pool.selectCustomPoolCount
import kotlinx.android.synthetic.main.fragment_select_custom_pool.selectCustomPoolList
import kotlinx.android.synthetic.main.fragment_select_custom_pool.selectCustomPoolRecommendedAction
import kotlinx.android.synthetic.main.fragment_select_custom_pool.selectCustomPoolToolbar
import kotlinx.coroutines.flow.first

class SelectCustomPoolFragment : BaseFragment<SelectCustomPoolViewModel>(), PoolAdapter.ItemHandler {

    companion object {

        const val PAYLOAD_KEY = "SelectCustomPoolFragment.Payload"

        fun getBundle(payload: SelectCustomPoolPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    val adapter by lazy {
        PoolAdapter(imageLoader, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_custom_pool, container, false)
    }

    override fun initViews() {
        selectCustomPoolToolbar.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        selectCustomPoolToolbar.setHomeButtonListener { viewModel.backClicked() }
        selectCustomPoolToolbar.addCustomAction(R.drawable.ic_search) {
            viewModel.searchClicked()
        }

        selectCustomPoolList.adapter = adapter
        selectCustomPoolList.setHasFixedSize(true)
        selectCustomPoolList.scrollToTopWhenItemsShuffled(viewLifecycleOwner)

        selectCustomPoolRecommendedAction.setOnClickListener { viewModel.selectRecommended() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .selectCustomPoolComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: SelectCustomPoolViewModel) {
        setupExternalActions(viewModel)

        viewModel.poolModelsFlow.observe(adapter::submitList)

        viewModel.selectedTitle.observe(selectCustomPoolCount::setText)

        viewModel.fillWithRecommendedEnabled.observe(selectCustomPoolRecommendedAction::setEnabled)
    }

    override fun poolInfoClicked(poolItem: PoolRvItem) {
        viewModel.poolInfoClicked(poolItem)
    }

    override fun poolClicked(poolItem: PoolRvItem) {
        viewModel.poolClicked(poolItem)
    }
}
