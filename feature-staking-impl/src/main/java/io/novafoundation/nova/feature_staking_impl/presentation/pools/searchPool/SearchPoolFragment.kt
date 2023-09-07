package io.novafoundation.nova.feature_staking_impl.presentation.pools.searchPool

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.scrollToTopWhenItemsShuffled
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.PoolAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.PoolRvItem
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.SelectingPoolPayload
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_search_pool.searchPoolCount
import kotlinx.android.synthetic.main.fragment_search_pool.searchPoolList
import kotlinx.android.synthetic.main.fragment_search_pool.searchPoolListHeader
import kotlinx.android.synthetic.main.fragment_search_pool.searchPoolPlaceholder
import kotlinx.android.synthetic.main.fragment_search_pool.searchPoolToolbar

class SearchPoolFragment : BaseFragment<SearchPoolViewModel>(), PoolAdapter.ItemHandler {

    companion object {

        const val PAYLOAD_KEY = "SearchPoolViewModel.Payload"

        fun getBundle(payload: SelectingPoolPayload): Bundle {
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
        return inflater.inflate(R.layout.fragment_search_pool, container, false)
    }

    override fun initViews() {
        searchPoolToolbar.applyStatusBarInsets()

        searchPoolToolbar.setHomeButtonListener { viewModel.backClicked() }

        searchPoolList.adapter = adapter
        searchPoolList.setHasFixedSize(true)
        searchPoolList.scrollToTopWhenItemsShuffled(viewLifecycleOwner)

        searchPoolToolbar.searchField.requestFocus()
        searchPoolToolbar.searchField.content.showSoftKeyboard()
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
        searchPoolToolbar.searchField.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.poolModelsFlow.observe {
            searchPoolListHeader.isInvisible = it.isEmpty()
            adapter.submitList(it)
        }

        viewModel.placeholderFlow.observe { placeholder ->
            searchPoolPlaceholder.isVisible = placeholder != null
            placeholder?.let { searchPoolPlaceholder.setModel(placeholder) }
        }

        viewModel.selectedTitle.observe(searchPoolCount::setText)
    }

    override fun poolInfoClicked(poolItem: PoolRvItem) {
        viewModel.poolClicked(poolItem)
    }

    override fun poolClicked(poolItem: PoolRvItem) {
        viewModel.poolClicked(poolItem)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        searchPoolToolbar.searchField.hideSoftKeyboard()
    }
}
