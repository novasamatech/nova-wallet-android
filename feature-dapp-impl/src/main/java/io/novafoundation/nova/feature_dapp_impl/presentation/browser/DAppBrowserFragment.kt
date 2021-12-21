package io.novafoundation.nova.feature_dapp_impl.presentation.browser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserWebView
import kotlinx.android.synthetic.main.fragment_dapp_main.dappMainContainer
import kotlinx.android.synthetic.main.fragment_dapp_main.dappMainIcon
import kotlinx.android.synthetic.main.fragment_dapp_main.dappMainSubId

class DAppBrowserFragment : BaseFragment<DAppBrowserViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_dapp_browser, container, false)
    }

    override fun initViews() {}

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .browserComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: DAppBrowserViewModel) {
        viewModel.polkadotJsExtension.inject(dappBrowserWebView)

        dappBrowserWebView.loadUrl("https://singular.rmrk.app")
    }
}
