package io.novafoundation.nova.feature_assets.presentation.tokens.add.selectChain

import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_assets.databinding.FragmentAddTokenSelectChainBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent

import javax.inject.Inject

class AddTokenSelectChainFragment :
    BaseFragment<AddTokenSelectChainViewModel, FragmentAddTokenSelectChainBinding>(),
    SelectChainAdapter.ItemHandler {

    override fun createBinding() = FragmentAddTokenSelectChainBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val chainsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        SelectChainAdapter(imageLoader, this)
    }

    override fun initViews() {
        binder.addTokenSelectChainToolbar.applyStatusBarInsets()

        binder.addTokenSelectChainChains.setHasFixedSize(true)
        binder.addTokenSelectChainChains.adapter = chainsAdapter

        binder.addTokenSelectChainToolbar.setHomeButtonListener { viewModel.backClicked() }
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
