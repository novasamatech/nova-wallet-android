package io.novafoundation.nova.feature_assets.presentation.send.flow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.asset.AssetGroupingDecoration
import io.novafoundation.nova.common.presentation.asset.applyDefaultTo
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.feature_nft_impl.di.NftFeatureComponent
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.flow.NftLinearAdapter
import io.novafoundation.nova.feature_nft_impl.domain.nft.search.SendNftListItem
import kotlinx.android.synthetic.main.fragment_nft_send_flow.nftFlowList
import kotlinx.android.synthetic.main.fragment_nft_send_flow.nftFlowPlaceholder
import kotlinx.android.synthetic.main.fragment_nft_send_flow.nftFlowSearch
import kotlinx.android.synthetic.main.fragment_nft_send_flow.nftFlowSearchContainer
import kotlinx.android.synthetic.main.fragment_nft_send_flow.nftFlowToolbar
import javax.inject.Inject

class NftSendFlowFragment : BaseFragment<NftSendFlowViewModel>(), NftLinearAdapter.ItemAssetHandler {

    @Inject
    lateinit var imageLoader: ImageLoader

    private val nftsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        NftLinearAdapter(imageLoader, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_nft_send_flow, container, false)

    override fun initViews() {
        nftFlowToolbar.setTitle(R.string.nft_send)
        nftFlowToolbar.applyStatusBarInsets()
        nftFlowToolbar.setHomeButtonListener { viewModel.backClicked() }
        nftFlowSearchContainer.applyInsetter {
            type(ime = true) {
                padding()
            }
        }

        with(nftFlowList) {
            setHasFixedSize(true)
            adapter = nftsAdapter

            AssetGroupingDecoration.applyDefaultTo(this, nftsAdapter)
            itemAnimator = null
        }

        nftFlowSearch.requestFocus()
        nftFlowSearch.content.showSoftKeyboard()
    }

    override fun inject() {
        FeatureUtils.getFeature<NftFeatureComponent>(this, NftFeatureApi::class.java)
            .nftSendFlowComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: NftSendFlowViewModel) {
        nftFlowSearch.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.searchResults.observe { searchResult ->
            nftFlowPlaceholder.setVisible(searchResult.isEmpty())
            nftFlowList.setVisible(searchResult.isNotEmpty())

            nftsAdapter.submitListPreservingViewPoint(
                data = searchResult,
                into = nftFlowList,
                extraDiffCompletedCallback = { nftFlowList.invalidateItemDecorations() }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        nftFlowSearch.hideSoftKeyboard()
    }

    override fun nftClicked(item: SendNftListItem) {
        viewModel.nftClicked(item)
    }
}
