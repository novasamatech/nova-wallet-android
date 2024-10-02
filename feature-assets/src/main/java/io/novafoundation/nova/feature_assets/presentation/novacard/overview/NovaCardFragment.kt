package io.novafoundation.nova.feature_assets.presentation.novacard.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.CookieManager
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.NovaCardEventHandler
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.NovaCardWebViewControllerFactory
import kotlinx.android.synthetic.main.fragment_nova_card.novaCardClearCookies
import kotlinx.android.synthetic.main.fragment_nova_card.novaCardContainer
import kotlinx.android.synthetic.main.fragment_nova_card.novaCardWebView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

class NovaCardFragment : BaseFragment<NovaCardViewModel>(), NovaCardEventHandler {

    @Inject
    lateinit var novaCardWebViewControllerFactory: NovaCardWebViewControllerFactory

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_nova_card, container, false)

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .novaCardComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun initViews() {
        novaCardContainer.applyStatusBarInsets()

        novaCardClearCookies.setOnClickListener {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
        }
    }

    override fun subscribe(viewModel: NovaCardViewModel) {
        viewModel.launch {
            val novaCardWebViewController = novaCardWebViewControllerFactory.create(
                fragment = this@NovaCardFragment,
                webView = novaCardWebView,
                eventHandler = this@NovaCardFragment,
                setupConfig = viewModel.setupCardConfig.first(),
                scope = viewModel
            )

            novaCardWebViewController.setup()
        }
    }

    override fun transactionStatusChanged(event: NovaCardEventHandler.TransactionStatus) {
        viewModel.onTransactionStatusChanged(event)
    }

    override fun openTopUp(amount: BigDecimal, address: String) {
        viewModel.openTopUp(amount, address)
    }
}
