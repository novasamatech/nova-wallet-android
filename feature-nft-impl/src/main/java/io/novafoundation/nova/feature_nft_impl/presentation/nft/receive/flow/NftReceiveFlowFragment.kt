package io.novafoundation.nova.feature_assets.presentation.receive.flow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.feature_nft_impl.di.NftFeatureComponent
import io.novafoundation.nova.feature_nft_impl.presentation.nft.receive.flow.NftChainsAdapter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.android.synthetic.main.fragment_nft_receive_flow.nftFlowList
import kotlinx.android.synthetic.main.fragment_nft_receive_flow.nftFlowSearchContainer
import kotlinx.android.synthetic.main.fragment_nft_receive_flow.nftFlowToolbar
import javax.inject.Inject

class NftReceiveFlowFragment : BaseFragment<NftReceiveFlowViewModel>(), NftChainsAdapter.Handler {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_nft_receive_flow, container, false)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val nftAdapter by lazy(LazyThreadSafetyMode.NONE) {
        NftChainsAdapter(imageLoader, this)
    }

    override fun initViews() {
        nftFlowToolbar.setTitle(R.string.nft_receive)
        nftFlowToolbar.applyStatusBarInsets()
        nftFlowToolbar.setHomeButtonListener { viewModel.backClicked() }
        nftFlowSearchContainer.applyInsetter {
            type(ime = true) {
                padding()
            }
        }

        with(nftFlowList) {
            setHasFixedSize(true)
            adapter = nftAdapter

            itemAnimator = null
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<NftFeatureComponent>(this, NftFeatureApi::class.java)
            .nftReceiveFlowComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: NftReceiveFlowViewModel) {
        viewModel.nftChainsFlow.observe { nftChains ->
            nftAdapter.submitListPreservingViewPoint(
                data = nftChains,
                into = nftFlowList,
                extraDiffCompletedCallback = { nftFlowList.invalidateItemDecorations() }
            )
        }
    }

    override fun itemClicked(item: Chain) {
        viewModel.chainSelected(item)
    }
}
