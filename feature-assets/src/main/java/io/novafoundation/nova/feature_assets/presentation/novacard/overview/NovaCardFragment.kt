package io.novafoundation.nova.feature_assets.presentation.novacard.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.NovaCardWebViewControllerFactory
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors.CardCreationInterceptor
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors.CardCreationInterceptorFactory
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors.TopUpRequestInterceptor
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors.TopUpRequestInterceptorFactory
import kotlinx.android.synthetic.main.fragment_nova_card.novaCardContainer
import kotlinx.android.synthetic.main.fragment_nova_card.novaCardWebView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

class NovaCardFragment : BaseFragment<NovaCardViewModel>(), CardCreationInterceptor.Callback, TopUpRequestInterceptor.Callback {

    @Inject
    lateinit var novaCardWebViewControllerFactory: NovaCardWebViewControllerFactory

    @Inject
    lateinit var cardCreationInterceptorFactory: CardCreationInterceptorFactory

    @Inject
    lateinit var topUpRequestInterceptorFactory: TopUpRequestInterceptorFactory

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
    }

    override fun subscribe(viewModel: NovaCardViewModel) {
        viewModel.launch {
            val interceptors = listOf(
                cardCreationInterceptorFactory.create(this@NovaCardFragment),
                topUpRequestInterceptorFactory.create(this@NovaCardFragment)
            )

            val novaCardWebViewController = novaCardWebViewControllerFactory.create(
                fragment = this@NovaCardFragment,
                webView = novaCardWebView,
                interceptors = interceptors,
                setupConfig = viewModel.setupCardConfig.first(),
                scope = viewModel
            )

            novaCardWebViewController.setup()
        }
    }

    override fun onCardCreated() {
        viewModel.onCardCreated()
    }

    override fun onCardTopUpCompleted(orderId: String) {
        viewModel.onTopUpFinished(orderId)
    }

    override fun onTopUpStart(orderId: String, amount: BigDecimal, address: String) {
        viewModel.openTopUp(orderId, amount, address)
    }
}
