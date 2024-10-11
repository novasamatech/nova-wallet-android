package io.novafoundation.nova.feature_nft_impl.presentation.nft.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.feature_nft_impl.di.NftFeatureComponent

import javax.inject.Inject

class NftListFragment : BaseFragment<NftListViewModel>(), NftAdapter.Handler {

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) { NftAdapter(imageLoader, this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_nft_list, container, false)
    }

    override fun initViews() {
        nftListToolbar.applyStatusBarInsets()
        nftListBack.setOnClickListener { viewModel.backClicked() }

        nftListNfts.setHasFixedSize(true)
        nftListNfts.adapter = adapter
        nftListNfts.itemAnimator = null

        nftListRefresh.setOnRefreshListener { viewModel.syncNfts() }
    }

    override fun inject() {
        FeatureUtils.getFeature<NftFeatureComponent>(this, NftFeatureApi::class.java)
            .nftListComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: NftListViewModel) {
        viewModel.nftListItemsFlow.observe {
            adapter.submitListPreservingViewPoint(it, nftListNfts)
        }

        viewModel.hideRefreshEvent.observeEvent {
            nftListRefresh.isRefreshing = false
        }

        viewModel.nftCountFlow.observe(nftListCounter::setText)
    }

    override fun itemClicked(item: NftListItem) {
        viewModel.nftClicked(item)
    }

    override fun loadableItemShown(item: NftListItem) {
        viewModel.loadableNftShown(item)
    }
}
