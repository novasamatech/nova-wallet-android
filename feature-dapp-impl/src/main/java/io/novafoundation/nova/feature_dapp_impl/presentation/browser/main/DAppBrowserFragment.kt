package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.SharedElementCallback
import androidx.core.os.bundleOf
import androidx.core.transition.addListener
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.databinding.FragmentDappBrowserBinding
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
import io.novafoundation.nova.common.utils.browser.fileChoosing.WebViewFileChooser
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder
import io.novafoundation.nova.common.utils.browser.permissions.WebViewPermissionAsker
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_deep_linking.presentation.handling.RootDeepLinkHandler
import io.novafoundation.nova.feature_external_sign_api.presentation.externalSign.AuthorizeDappBottomSheet
import javax.inject.Inject

private const val OVERFLOW_TABS_COUNT = 100

const val DAPP_SHARED_ELEMENT_ID_IMAGE_TAB = "DAPP_SHARED_ELEMENT_ID_IMAGE_TAB"

class DAppBrowserFragment : BaseFragment<DAppBrowserViewModel, FragmentDappBrowserBinding>(), OptionsBottomSheetDialog.Callback, PageCallback {

    companion object {

        private const val PAYLOAD = "DAppBrowserFragment.Payload"

        fun getBundle(payload: DAppBrowserPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun createBinding() = FragmentDappBrowserBinding.inflate(layoutInflater)

    @Inject
    lateinit var compoundWeb3Injector: CompoundWeb3Injector

    @Inject
    lateinit var webViewHolder: WebViewHolder

    @Inject
    lateinit var fileChooser: WebViewFileChooser

    @Inject
    lateinit var permissionAsker: WebViewPermissionAsker

    @Inject
    lateinit var imageLoader: ImageLoader

    private var webViewClient: Web3WebViewClient? = null

    var backCallback: OnBackPressedCallback? = null

    private val dappBrowserWebView: WebView?
        get() {
            return binder.dappBrowserWebViewContainer.getChildAt(0) as? WebView
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        WebView.enableSlowWholeDocumentDraw()
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move).apply {
                addListener(
                    onStart = { binder.dappBrowserWebViewContainer.makeGone() }, // Hide WebView during transition animation
                    onEnd = {
                        binder.dappBrowserWebViewContainer.makeVisible()
                        binder.dappBrowserTransitionImage.animate()
                            .setDuration(300)
                            .alpha(0f)
                            .withEndAction { binder.dappBrowserTransitionImage.makeGone() }
                            .start()
                    }
                )
            }
    }

    override fun initViews() {
        binder.dappBrowserAddressBarGroup.applyStatusBarInsets()

        binder.dappBrowserHide.setOnClickListener { viewModel.closeClicked() }

        binder.dappBrowserBack.setOnClickListener { backClicked() }

        binder.dappBrowserAddressBar.setOnClickListener {
            viewModel.openSearch()
        }

        binder.dappBrowserForward.setOnClickListener { forwardClicked() }
        binder.dappBrowserTabs.setOnClickListener { viewModel.openTabs() }
        binder.dappBrowserRefresh.setOnClickListener { refreshClicked() }
        binder.dappBrowserFavorite.setOnClickListener { viewModel.onFavoriteClick() }
        binder.dappBrowserMore.setOnClickListener { moreClicked() }

        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER

        binder.dappBrowserTransitionImage.transitionName = DAPP_SHARED_ELEMENT_ID_IMAGE_TAB

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onSharedElementStart(
                sharedElementNames: MutableList<String>?,
                sharedElements: MutableList<View>?,
                sharedElementSnapshots: MutableList<View>?
            ) {
                val sharedView = sharedElements?.firstOrNull { it.transitionName == DAPP_SHARED_ELEMENT_ID_IMAGE_TAB }
                val sharedImageView = sharedView as? ImageView
                binder.dappBrowserTransitionImage.setImageDrawable(sharedImageView?.drawable) // Set image from shared element
            }
        })
    }

    override fun onDestroyView() {
        binder.dappBrowserWebViewContainer.removeAllViews()
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

    @SuppressLint("SetTextI18n")
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
            binder.dappBrowserAddressBar.setAddress(it.display)
            binder.dappBrowserAddressBar.showSecure(it.isSecure)
            binder.dappBrowserFavorite.setImageResource(favoriteIcon(it.isFavourite))

            updateButtonsState()
        }

        viewModel.tabsCountFlow.observe {
            if (it >= OVERFLOW_TABS_COUNT) {
                binder.dappBrowserTabsIcon.makeVisible()
                binder.dappBrowserTabsContent.text = null
            } else {
                binder.dappBrowserTabsIcon.makeGone()
                binder.dappBrowserTabsContent.text = it.toString()
            }
        }

        viewModel.openDeeplinkViaSystem.observeEvent(::openDeeplinkViaSystem)
    }

    private fun attachSession(session: BrowserTabSession) {
        clearProgress()
        session.attachToHost(createChromeClient(), this)
        webViewHolder.set(session.webView)
        webViewClient = session.webViewClient

        binder.dappBrowserWebViewContainer.removeAllViews()
        binder.dappBrowserWebViewContainer.addView(session.webView)
    }

    private fun clearProgress() {
        binder.dappBrowserProgress.makeGone()
        binder.dappBrowserProgress.progress = 0
    }

    private fun createChromeClient() = Web3ChromeClient(permissionAsker, fileChooser, viewModel.viewModelScope, binder.dappBrowserProgress)

    private fun updateButtonsState() {
        binder.dappBrowserForward.isEnabled = dappBrowserWebView?.canGoForward() ?: false
        binder.dappBrowserBack.isEnabled = dappBrowserWebView?.canGoBack() ?: false
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

    override fun handleBrowserDeeplink(uri: Uri) {
        viewModel.onBrowserDeeplinkOpened(uri)
    }

    override fun onPageChanged(webView: WebView, url: String?, title: String?) {
        viewModel.onPageChanged(url, title)
    }

    private fun openDeeplinkViaSystem(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.common_no_app_to_handle_intent, Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun favoriteIcon(isFavorite: Boolean): Int {
        return if (isFavorite) {
            R.drawable.ic_favorite_heart_filled
        } else {
            R.drawable.ic_favorite_heart_outline
        }
    }
}
