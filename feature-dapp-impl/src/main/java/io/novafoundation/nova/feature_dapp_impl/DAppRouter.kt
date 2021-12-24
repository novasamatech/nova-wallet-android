package io.novafoundation.nova.feature_dapp_impl

import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicPayload

interface DAppRouter {

    fun openChangeAccount()

    fun openDAppBrowser()

    fun openConfirmSignExtrinsic(payload: DAppSignExtrinsicPayload)
}
