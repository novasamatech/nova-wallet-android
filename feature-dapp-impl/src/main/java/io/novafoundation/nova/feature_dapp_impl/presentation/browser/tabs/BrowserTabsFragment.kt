package io.novafoundation.nova.feature_dapp_impl.presentation.browser.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_dapp_tabs.tabList

class BrowserTabsFragment : BaseFragment<BrowserTabsViewModel>() {

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        BrowserTabsAdapter(imageLoader) { viewModel.onTabClick(it.id) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_dapp_tabs, container, false)
    }

    override fun initViews() {
        tabList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .browserTabsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: BrowserTabsViewModel) {
        viewModel.tabs.observe { tabs ->
            adapter.submitList(tabs)
        }
    }
}
