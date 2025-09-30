package io.novafoundation.nova.feature_nft_impl.presentation.nft.list

import android.view.View
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.insets.applyNavigationBarInsets
import io.novafoundation.nova.common.utils.insets.applyStatusBarInsets
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_impl.databinding.FragmentNftListBinding
import io.novafoundation.nova.feature_nft_impl.di.NftFeatureComponent

import javax.inject.Inject

class NftListFragment : BaseFragment<NftListViewModel, FragmentNftListBinding>(), NftAdapter.Handler {

    override fun createBinding() = FragmentNftListBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) { NftAdapter(imageLoader, this) }

    override fun applyInsets(rootView: View) {
        binder.nftListToolbar.applyStatusBarInsets()
        binder.nftListNfts.applyNavigationBarInsets()
    }

    override fun initViews() {
        binder.nftListBack.setOnClickListener { viewModel.backClicked() }

        binder.nftListNfts.setHasFixedSize(true)
        binder.nftListNfts.adapter = adapter
        binder.nftListNfts.itemAnimator = null

        binder.nftListRefresh.setOnRefreshListener { viewModel.syncNfts() }
    }

    override fun inject() {
        FeatureUtils.getFeature<NftFeatureComponent>(this, NftFeatureApi::class.java)
            .nftListComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: NftListViewModel) {
        viewModel.nftListItemsFlow.observe {
            adapter.submitListPreservingViewPoint(it, binder.nftListNfts)
        }

        viewModel.hideRefreshEvent.observeEvent {
            binder.nftListRefresh.isRefreshing = false
        }

        viewModel.nftCountFlow.observe(binder.nftListCounter::setText)
    }

    override fun itemClicked(item: NftListItem) {
        viewModel.nftClicked(item)
    }

    override fun loadableItemShown(item: NftListItem) {
        viewModel.loadableNftShown(item)
    }
}
