package io.novafoundation.nova.feature_assets.presentation.send.phishing

import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.feature_assets.presentation.send.phishing.warning.api.PhishingWarningPresentation

fun <T> BaseFragment<T>.observePhishingCheck(
    viewModel: T
) where T : BaseViewModel, T : PhishingWarningPresentation {
    viewModel.showPhishingWarning.observeEvent {
        showPhishingWarning(viewModel, it)
    }
}

private fun <T> BaseFragment<T>.showPhishingWarning(
    viewModel: T,
    address: String
) where T : BaseViewModel, T : PhishingWarningPresentation {
    warningDialog(
        requireContext(),
        { viewModel.proceedAddress(address) },
        viewModel::declinePhishingAddress
    ) {
        setTitle(R.string.wallet_send_phishing_warning_title)
        setMessage(getString(R.string.wallet_send_phishing_warning_text, address))
    }
}
