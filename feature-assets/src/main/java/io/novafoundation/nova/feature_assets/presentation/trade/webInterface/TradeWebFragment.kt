package io.novafoundation.nova.feature_assets.presentation.trade.webInterface

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.feature_assets.databinding.FragmentTradeWebInterfaceBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent

class TradeWebFragment : BaseFragment<TradeWebViewModel, FragmentTradeWebInterfaceBinding>() {

    companion object : PayloadCreator<TradeWebPayload> by FragmentPayloadCreator()

    override fun createBinding() = FragmentTradeWebInterfaceBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.tradeWebToolbar.applyStatusBarInsets()
        binder.tradeWebToolbar.setHomeButtonListener { viewModel.back() }

        binder.tradeWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            displayZoomControls = false
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .tradeWebComponent()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: TradeWebViewModel) {
        viewModel.integrator.observe {
            it.run(binder.tradeWebView)
        }

        viewModel.webChromeClientFlow.observeFirst {
            binder.tradeWebView.webChromeClient = it
        }
    }
}
