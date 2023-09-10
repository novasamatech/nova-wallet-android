package io.novafoundation.nova.feature_assets.presentation.tokens.add.selectChain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import kotlinx.android.synthetic.main.fragment_add_token_select_chain.addTokenSelectChainChains
import kotlinx.android.synthetic.main.fragment_add_token_select_chain.addTokenSelectChainToolbar
import javax.inject.Inject

class AddTokenSelectChainFragment :
    BaseFragment<AddTokenSelectChainViewModel>(),
    SelectChainAdapter.ItemHandler {

    @Inject
    lateinit var imageLoader: ImageLoader

    private val erc20TokensTitleAdapter by lazy(LazyThreadSafetyMode.NONE) {
        SelectChainGroupAdapter(R.string.assets_add_token_select_erc20_chain_title)
    }
    private val erc20TokensAdapter by lazy(LazyThreadSafetyMode.NONE) {
        SelectChainAdapter(imageLoader, this, isEthereumBased = true)
    }
    private val substrateTokensTitleAdapter by lazy(LazyThreadSafetyMode.NONE) {
        SelectChainGroupAdapter(R.string.assets_add_token_select_substrate_chain_title)
    }
    private val substrateTokensAdapter by lazy(LazyThreadSafetyMode.NONE) {
        SelectChainAdapter(imageLoader, this, isEthereumBased = false)
    }
    private val selectChainAdapter by lazy(LazyThreadSafetyMode.NONE) {
        ConcatAdapter(
            erc20TokensTitleAdapter, erc20TokensAdapter,
            substrateTokensTitleAdapter, substrateTokensAdapter
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_add_token_select_chain, container, false)
    }

    override fun initViews() {
        addTokenSelectChainToolbar.applyStatusBarInsets()
        addTokenSelectChainToolbar.setTitle(R.string.assets_add_token_select_chain_title)

        addTokenSelectChainChains.setHasFixedSize(true)
        addTokenSelectChainChains.adapter = selectChainAdapter

        addTokenSelectChainToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .addTokenSelectChainComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: AddTokenSelectChainViewModel) {
        viewModel.availableEthereumChainModels.observe(erc20TokensAdapter::submitList)
        viewModel.availableSubstrateChainModels.observe(substrateTokensAdapter::submitList)
    }

    override fun itemClicked(position: Int, isEthereumBased: Boolean) {
        viewModel.chainClicked(position, isEthereumBased)
    }
}
