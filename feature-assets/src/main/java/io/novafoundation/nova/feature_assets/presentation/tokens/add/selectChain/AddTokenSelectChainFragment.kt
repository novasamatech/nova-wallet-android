package io.novafoundation.nova.feature_assets.presentation.tokens.add.selectChain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private val chainsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        SelectChainAdapter(imageLoader, this)
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

        addTokenSelectChainChains.setHasFixedSize(true)
        addTokenSelectChainChains.adapter = chainsAdapter

        addTokenSelectChainToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .addTokenSelectChainComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: AddTokenSelectChainViewModel) {
        viewModel.availableChainModels.observe(chainsAdapter::submitList)
    }

    override fun itemClicked(position: Int) {
        viewModel.chainClicked(position)
    }
}
