package io.novafoundation.nova.app.root.presentation.main.swaps

import io.novafoundation.nova.app.databinding.FragmentSwapsTabBinding
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootComponent
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils

// The new swap screen is delivered separately. Keep this destination as an empty tab root for now.
class SwapsTabFragment : BaseFragment<SwapsTabViewModel, FragmentSwapsTabBinding>() {

    override fun createBinding() = FragmentSwapsTabBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<RootComponent>(this, RootApi::class.java)
            .swapsTabComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun initViews() = Unit

    override fun subscribe(viewModel: SwapsTabViewModel) = Unit
}
