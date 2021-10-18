package jp.co.soramitsu.feature_account_api.presenatation.actions

import jp.co.soramitsu.common.base.BaseFragment
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.mixin.impl.observeBrowserEvents

fun <T> BaseFragment<T>.setupExternalActions(viewModel: T) where T : BaseViewModel, T : ExternalActions {
    observeBrowserEvents(viewModel)

    viewModel.showExternalActionsEvent.observeEvent {
        showExternalActions(it, viewModel)
    }
}

fun <T> BaseFragment<T>.showExternalActions(
    payload: ExternalActions.Payload,
    viewModel: T
) where T : BaseViewModel, T : ExternalActions {
    ExternalActionsSheet(
        requireContext(),
        payload,
        viewModel::copyAddressClicked,
        viewModel::viewExternalClicked
    ).show()
}

fun <T> T.copyAddressClicked(address: String) where T : BaseViewModel, T : ExternalActions {
    copyAddress(address, ::showMessage)
}
