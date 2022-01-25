package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.themed
import io.novafoundation.nova.common.view.dialog.dialog
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DappPendingConfirmation.Action
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.sheets.ConfirmAuthorizeBottomSheet
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3WebViewClientFactory
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder
import io.novafoundation.nova.feature_dapp_impl.web3.webview.injectWeb3
import io.novafoundation.nova.feature_dapp_impl.web3.webview.uninjectWeb3
import kotlinx.android.synthetic.main.fragment_dapp_browser.*
import javax.inject.Inject

class DAppBrowserFragment : BaseFragment<DAppBrowserViewModel>() {

    companion object {

        private const val PAYLOAD = "DAppBrowserFragment.Payload"

        fun getBundle(initialUrl: String) = bundleOf(PAYLOAD to initialUrl)
    }

    @Inject
    lateinit var web3WebViewClientFactory: Web3WebViewClientFactory

    @Inject
    lateinit var webViewHolder: WebViewHolder

    @Inject
    lateinit var imageLoader: ImageLoader

    var backCallback: OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_dapp_browser, container, false)
    }

    override fun initViews() {
        webViewHolder.set(dappBrowserWebView)

        dappBrowserAddressBarGroup.applyStatusBarInsets()

        dappBrowserClose.setOnClickListener { viewModel.closeClicked() }

        dappBrowserBack.setOnClickListener { backClicked() }
        attachBackCallback()

        dappBrowserAddressBar.setOnClickListener {
            viewModel.openSearch()
        }

        dappBrowserForward.setOnClickListener { forwardClicked() }
        dappBrowserRefresh.setOnClickListener { refreshClicked() }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        detachBackCallback()
        dappBrowserWebView.uninjectWeb3()

        webViewHolder.release()
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
        dappBrowserWebView.injectWeb3(web3WebViewClientFactory, viewModel::onPageChanged)

        viewModel.showConfirmationSheet.observeEvent {
            when (it.action) {
                is Action.Authorize -> {
                    showConfirmAuthorizeSheet(it as DappPendingConfirmation<Action.Authorize>)
                }
                Action.CloseScreen -> showCloseConfirmation(it)
            }
        }

        viewModel.loadUrlEvent.observeEvent(dappBrowserWebView::loadUrl)

        viewModel.currentPage.observe {
            dappBrowserAddressBar.setAddress(it.display)
            dappBrowserAddressBar.showSecureIcon(it.isSecure)

            updateButtonsState()
        }
    }

    private fun showCloseConfirmation(pendingConfirmation: DappPendingConfirmation<*>) {
        dialog(requireContext().themed(R.style.AccentAlertDialogTheme_Reversed)) {
            setPositiveButton(R.string.common_close) { _, _ -> pendingConfirmation.onConfirm() }
            setNegativeButton(R.string.common_cancel) { _, _ -> pendingConfirmation.onCancel() }

            setTitle(R.string.common_confirmation_title)
            setMessage(R.string.dapp_browser_close_warning_message)
        }
    }

    private fun updateButtonsState() {
        dappBrowserForward.isEnabled = dappBrowserWebView.canGoForward()
        dappBrowserBack.isEnabled = dappBrowserWebView.canGoBack()
    }

    private fun showConfirmAuthorizeSheet(pendingConfirmation: DappPendingConfirmation<Action.Authorize>) {
        ConfirmAuthorizeBottomSheet(
            context = requireContext(),
            confirmation = pendingConfirmation,
            imageLoader = imageLoader
        ).show()
    }

    private fun backClicked() {
        if (dappBrowserWebView.canGoBack()) {
            dappBrowserWebView.goBack()
        } else {
            viewModel.closeClicked()
        }
    }

    private fun forwardClicked() {
        dappBrowserWebView.goForward()
    }

    private fun refreshClicked() {
        dappBrowserWebView.reload()
    }

    private fun attachBackCallback() {
        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backClicked()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(backCallback!!)
    }

    private fun detachBackCallback() {
        backCallback?.remove()
        backCallback = null
    }
}
