package io.novafoundation.nova.app.root.presentation

import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload

interface RootRouter {

    fun returnToWallet()

    fun nonCancellableVerify()

    fun openUpdateNotifications()
}
