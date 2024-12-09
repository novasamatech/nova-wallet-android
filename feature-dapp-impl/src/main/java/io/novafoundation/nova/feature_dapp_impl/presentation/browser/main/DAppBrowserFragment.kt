package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.SharedElementCallback
import androidx.core.os.bundleOf
import androidx.core.transition.addListener
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.domain.browser.isSecure
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DappPendingConfirmation.Action
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.sheets.AcknowledgePhishingBottomSheet
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.OptionsBottomSheetDialog
import io.novafoundation.nova.feature_dapp_impl.presentation.common.favourites.setupRemoveFavouritesConfirmation
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTabSession
import io.novafoundation.nova.feature_dapp_impl.web3.webview.PageCallback
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3ChromeClient
import io.novafoundation.nova.feature_dapp_impl.web3.webview.CompoundWeb3Injector
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3WebViewClient
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewFileChooser
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder
import io.novafoundation.nova.feature_external_sign_api.presentation.externalSign.AuthorizeDappBottomSheet
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserAddressBar
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserAddressBarGroup
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserBack
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserHide
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserForward
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserMore
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserProgress
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserRefresh
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserFavorite
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserTabs
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserTabsContent
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserTabsIcon
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserTransitionImage
import kotlinx.android.synthetic.main.fragment_dapp_browser.dappBrowserWebViewContainer

private const val OVERFLOW_TABS_COUNT = 100

const val DAPP_SHARED_ELEMENT_ID_IMAGE_TAB = "DAPP_SHARED_ELEMENT_ID_IMAGE_TAB"

class DAppBrowserFragment : BaseFragment<DAppBrowserViewModel>(), OptionsBottomSheetDialog.Callback, PageCallback {

    companion object {

        private const val PAYLOAD = "DAppBrowserFragment.Payload"

        fun getBundle(payload: DAppBrowserPayload) = bundleOf(PAYLOAD to payload)
    }

    @Inject
    lateinit var compoundWeb3Injector: CompoundWeb3Injector

    @Inject
    lateinit var webViewHolder: WebViewHolder

    @Inject
    lateinit var fileChooser: WebViewFileChooser

    @Inject
    lateinit var imageLoader: ImageLoader

    private var webViewClient: Web3WebViewClient? = null

    var backCallback: OnBackPressedCallback? = null

    private val dappBrowserWebView: WebView?
        get() {
            return dappBrowserWebViewContainer.getChildAt(0) as? WebView
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        WebView.enableSlowWholeDocumentDraw()
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move).apply {
                addListener(
                    onStart = { dappBrowserWebViewContainer.makeGone() }, // Hide WebView during transition animation
                    onEnd = {
                        dappBrowserWebViewContainer.makeVisible()
                        dappBrowserTransitionImage.animate()
                            .setDuration(300)
                            .alpha(0f)
                            .withEndAction { dappBrowserTransitionImage.makeGone() }
                            .start()
                    }
                )
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_dapp_browser, container, false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        fileChooser.onActivityResult(requestCode, resultCode, data)
    }

    override fun initViews() {
        dappBrowserAddressBarGroup.applyStatusBarInsets()

        dappBrowserHide.setOnClickListener { viewModel.closeClicked() }

        dappBrowserBack.setOnClickListener { backClicked() }

        dappBrowserAddressBar.setOnClickListener {
            viewModel.openSearch()
        }

        dappBrowserForward.setOnClickListener { forwardClicked() }
        dappBrowserTabs.setOnClickListener { viewModel.openTabs() }
        dappBrowserRefresh.setOnClickListener { refreshClicked() }
        dappBrowserFavorite.setOnClickListener { viewModel.onFavoriteClick() }
        dappBrowserMore.setOnClickListener { moreClicked() }

        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER

        dappBrowserTransitionImage.transitionName = DAPP_SHARED_ELEMENT_ID_IMAGE_TAB

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onSharedElementStart(
                sharedElementNames: MutableList<String>?,
                sharedElements: MutableList<View>?,
                sharedElementSnapshots: MutableList<View>?
            ) {
                val sharedView = sharedElements?.firstOrNull { it.transitionName == DAPP_SHARED_ELEMENT_ID_IMAGE_TAB }
                val sharedImageView = sharedView as? ImageView
                dappBrowserTransitionImage.setImageDrawable(sharedImageView?.drawable) // Set image from shared element
            }
        })
    }

    override fun onDestroyView() {
        dappBrowserWebViewContainer.removeAllViews()
        viewModel.detachCurrentSession()
        super.onDestroyView()

        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onPause() {
        super.onPause()
        viewModel.makePageSnapshot()

        detachBackCallback()
    }

    override fun onResume() {
        super.onResume()
        attachBackCallback()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden) {
            detachBackCallback()
        } else {
            attachBackCallback()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .browserComponentFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun subscribe(viewModel: DAppBrowserViewModel) {
        setupRemoveFavouritesConfirmation(viewModel.removeFromFavouritesConfirmation)

        viewModel.currentTabFlow.observe { currentTab ->
            attachSession(currentTab.browserTabSession)
        }

        viewModel.desktopModeChangedModel.observe {
            webViewClient?.desktopMode = it.desktopModeEnabled
        }

        viewModel.showConfirmationSheet.observeEvent {
            when (it.action) {
                is Action.Authorize -> {
                    showConfirmAuthorizeSheet(it as DappPendingConfirmation<Action.Authorize>)
                }

                Action.AcknowledgePhishingAlert -> {
                    AcknowledgePhishingBottomSheet(requireContext(), it)
                        .show()
                }
            }
        }

        viewModel.browserCommandEvent.observeEvent {
            when (it) {
                BrowserCommand.Reload -> dappBrowserWebView?.reload()
                BrowserCommand.GoBack -> backClicked()
                is BrowserCommand.OpenUrl -> dappBrowserWebView?.loadUrl(it.url)
                is BrowserCommand.ChangeDesktopMode -> {
                    webViewClient?.desktopMode = it.enabled
                    dappBrowserWebView?.reload()
                }
            }
        }

        viewModel.openBrowserOptionsEvent.observeEvent {
            val optionsBottomSheet = OptionsBottomSheetDialog(requireContext(), this, it)
            optionsBottomSheet.show()
        }

        viewModel.currentPageAnalyzed.observe {
            dappBrowserAddressBar.setAddress(it.display)
            dappBrowserAddressBar.showSecure(it.isSecure)
            dappBrowserFavorite.setImageResource(favoriteIcon(it.isFavourite))

            updateButtonsState()
        }

        viewModel.tabsCountFlow.observe {
            if (it >= OVERFLOW_TABS_COUNT) {
                dappBrowserTabsIcon.makeVisible()
                dappBrowserTabsContent.text = null
            } else {
                dappBrowserTabsIcon.makeGone()
                dappBrowserTabsContent.text = it.toString()
            }
        }
    }

    private fun attachSession(session: BrowserTabSession) {
        clearProgress()
        session.attachToHost(createChromeClient(), this)
        webViewHolder.set(session.webView)
        webViewClient = session.webViewClient

        dappBrowserWebViewContainer.removeAllViews()
        dappBrowserWebViewContainer.addView(session.webView)
    }

    private fun clearProgress() {
        dappBrowserProgress.makeGone()
        dappBrowserProgress.progress = 0
    }

    private fun createChromeClient() = Web3ChromeClient(fileChooser, dappBrowserProgress)

    private fun updateButtonsState() {
        dappBrowserForward.isEnabled = dappBrowserWebView?.canGoForward() ?: false
        dappBrowserBack.isEnabled = dappBrowserWebView?.canGoBack() ?: false
    }

    private fun showConfirmAuthorizeSheet(pendingConfirmation: DappPendingConfirmation<Action.Authorize>) {
        AuthorizeDappBottomSheet(
            context = requireContext(),
            onConfirm = pendingConfirmation.onConfirm,
            onDeny = pendingConfirmation.onDeny,
            payload = pendingConfirmation.action.content,
        ).show()
    }

    private fun backClicked() {
        if (dappBrowserWebView?.canGoBack() == true) {
            dappBrowserWebView?.goBack()
        } else {
            viewModel.closeClicked()
        }
    }

    private fun forwardClicked() {
        dappBrowserWebView?.goForward()
    }

    private fun refreshClicked() {
        dappBrowserWebView?.reload()
    }

    private fun attachBackCallback() {
        if (backCallback == null) {
            backCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backClicked()
                }
            }
            requireActivity().onBackPressedDispatcher.addCallback(backCallback!!)
        }
    }

    private fun moreClicked() {
        viewModel.onMoreClicked()
    }

    private fun detachBackCallback() {
        backCallback?.remove()
        backCallback = null
    }

    override fun onDesktopModeClick() {
        viewModel.onDesktopClick()
    }

    override fun onPageStarted(webView: WebView, url: String, favicon: Bitmap?) {
        compoundWeb3Injector.injectForPage(webView, viewModel.extensionsStore)
    }

    override fun handleBrowserIntent(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.common_no_app_to_handle_intent, Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun onPageChanged(webView: WebView, url: String, title: String?) {
        viewModel.onPageChanged(url, title)
    }

    private fun favoriteIcon(isFavorite: Boolean): Int {
        return if (isFavorite) {
            R.drawable.ic_favorite_heart_filled
        } else {
            R.drawable.ic_favorite_heart_outline
        }
    }
}
