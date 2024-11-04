package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main

import android.content.Intent
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.themed
import io.novafoundation.nova.common.view.dialog.dialog
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.databinding.FragmentDappBrowserBinding
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.domain.browser.isSecure
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DappPendingConfirmation.Action
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.sheets.AcknowledgePhishingBottomSheet
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.OptionsBottomSheetDialog
import io.novafoundation.nova.feature_dapp_impl.presentation.common.favourites.setupRemoveFavouritesConfirmation
import io.novafoundation.nova.feature_dapp_impl.web3.webview.PageCallback
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3WebViewClient
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3WebViewClientFactory
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewFileChooser
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder
import io.novafoundation.nova.feature_dapp_impl.web3.webview.injectWeb3
import io.novafoundation.nova.feature_dapp_impl.web3.webview.uninjectWeb3
import io.novafoundation.nova.feature_external_sign_api.presentation.externalSign.AuthorizeDappBottomSheet

import javax.inject.Inject

class DAppBrowserFragment : BaseFragment<DAppBrowserViewModel, FragmentDappBrowserBinding>(), OptionsBottomSheetDialog.Callback, PageCallback {

    companion object {

        private const val PAYLOAD = "DAppBrowserFragment.Payload"

        fun getBundle(initialUrl: String) = bundleOf(PAYLOAD to initialUrl)
    }

    override fun createBinding() = FragmentDappBrowserBinding.inflate(layoutInflater)

    @Inject
    lateinit var web3WebViewClientFactory: Web3WebViewClientFactory

    @Inject
    lateinit var webViewHolder: WebViewHolder

    @Inject
    lateinit var fileChooser: WebViewFileChooser

    private var webViewClient: Web3WebViewClient? = null

    var backCallback: OnBackPressedCallback? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        fileChooser.onActivityResult(requestCode, resultCode, data)
    }

    override fun initViews() {
        webViewHolder.set(binder.dappBrowserWebView)

        binder.dappBrowserAddressBarGroup.applyStatusBarInsets()

        binder.dappBrowserClose.setOnClickListener { viewModel.closeClicked() }

        binder.dappBrowserBack.setOnClickListener { backClicked() }

        binder.dappBrowserAddressBar.setOnClickListener {
            viewModel.openSearch()
        }

        binder.dappBrowserForward.setOnClickListener { forwardClicked() }
        binder.dappBrowserRefresh.setOnClickListener { refreshClicked() }

        binder.dappBrowserMore.setOnClickListener { moreClicked() }

        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binder.dappBrowserWebView.uninjectWeb3()

        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        webViewHolder.release()
    }

    override fun onPause() {
        super.onPause()
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

        webViewClient = web3WebViewClientFactory.create(binder.dappBrowserWebView, viewModel.extensionsStore, viewModel::onPageChanged, this)
        binder.dappBrowserWebView.injectWeb3(
            progressBar = binder.dappBrowserProgress,
            fileChooser = fileChooser,
            web3Client = webViewClient!!
        )

        viewModel.desktopModeChangedModel.observe {
            webViewClient?.desktopMode = it.desktopModeEnabled
        }

        viewModel.showConfirmationSheet.observeEvent {
            when (it.action) {
                is Action.Authorize -> {
                    showConfirmAuthorizeSheet(it as DappPendingConfirmation<Action.Authorize>)
                }
                Action.CloseScreen -> showCloseConfirmation(it)
                Action.AcknowledgePhishingAlert -> {
                    AcknowledgePhishingBottomSheet(requireContext(), it)
                        .show()
                }
            }
        }

        viewModel.browserCommandEvent.observeEvent {
            when (it) {
                BrowserCommand.Reload -> binder.dappBrowserWebView.reload()
                BrowserCommand.GoBack -> backClicked()
                is BrowserCommand.OpenUrl -> binder.dappBrowserWebView.loadUrl(it.url)
                is BrowserCommand.ChangeDesktopMode -> {
                    webViewClient?.desktopMode = it.enabled
                    binder.dappBrowserWebView.reload()
                }
            }
        }

        viewModel.openBrowserOptionsEvent.observeEvent {
            val optionsBottomSheet = OptionsBottomSheetDialog(requireContext(), this, it)
            optionsBottomSheet.show()
        }

        viewModel.currentPageAnalyzed.observe {
            binder.dappBrowserAddressBar.setAddress(it.display)
            binder.dappBrowserAddressBar.showSecureIcon(it.isSecure)

            updateButtonsState()
        }
    }

    private fun showCloseConfirmation(pendingConfirmation: DappPendingConfirmation<*>) {
        dialog(requireContext().themed(R.style.AccentNegativeAlertDialogTheme_Reversed)) {
            setPositiveButton(R.string.common_close) { _, _ -> pendingConfirmation.onConfirm() }
            setNegativeButton(R.string.common_cancel) { _, _ -> pendingConfirmation.onCancel() }

            setTitle(R.string.common_confirmation_title)
            setMessage(R.string.common_close_confirmation_message)
        }
    }

    private fun updateButtonsState() {
        binder.dappBrowserForward.isEnabled = binder.dappBrowserWebView.canGoForward()
        binder.dappBrowserBack.isEnabled = binder.dappBrowserWebView.canGoBack()
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
        if (binder.dappBrowserWebView.canGoBack()) {
            binder.dappBrowserWebView.goBack()
        } else {
            viewModel.closeClicked()
        }
    }

    private fun forwardClicked() {
        binder.dappBrowserWebView.goForward()
    }

    private fun refreshClicked() {
        binder.dappBrowserWebView.reload()
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

    override fun onFavoriteClick(payload: DAppOptionsPayload) {
        viewModel.onFavoriteClick(payload)
    }

    override fun onDesktopModeClick() {
        viewModel.onDesktopClick()
    }

    override fun handleBrowserIntent(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.common_no_app_to_handle_intent, Toast.LENGTH_LONG)
                .show()
        }
    }
}
