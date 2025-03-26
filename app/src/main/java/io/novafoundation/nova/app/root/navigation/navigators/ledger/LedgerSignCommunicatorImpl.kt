package io.novafoundation.nova.app.root.navigation.navigators.ledger

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.getBackStackEntryBefore
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator.Request
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator.Response
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.sign.SignLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.sign.SignLedgerPayload

class LedgerSignCommunicatorImpl(navigationHoldersRegistry: NavigationHoldersRegistry) :
    NavStackInterScreenCommunicator<Request, Response>(navigationHoldersRegistry), LedgerSignCommunicator {

    private var usedVariant: LedgerVariant? = null

    override fun respond(response: Response) {
        val requester = navController.getBackStackEntryBefore(R.id.signLedgerFragment)

        saveResultTo(requester, response)
    }

    override fun setUsedVariant(variant: LedgerVariant) {
        usedVariant = variant
    }

    override fun openRequest(request: Request) {
        super.openRequest(request)

        val payload = SignLedgerPayload(request, requireNotNull(usedVariant), SelectLedgerPayload.ConnectionMode.ALL)
        val bundle = SignLedgerFragment.getBundle(payload)
        navController.navigate(R.id.action_open_sign_ledger, bundle)
    }
}
