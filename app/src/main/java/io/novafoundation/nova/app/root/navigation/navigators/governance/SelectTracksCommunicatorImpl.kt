package io.novafoundation.nova.app.root.navigation.navigators.governance

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksRequester
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksResponder
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.governanceTracks.SelectGovernanceTracksFragment

class SelectTracksCommunicatorImpl(private val router: GovernanceRouter, navigationHolder: SplitScreenNavigationHolder) :
    NavStackInterScreenCommunicator<SelectTracksRequester.Request, SelectTracksResponder.Response>(navigationHolder),
    SelectTracksCommunicator {

    override fun openRequest(request: SelectTracksRequester.Request) {
        super.openRequest(request)

        router.openSelectGovernanceTracks(SelectGovernanceTracksFragment.getBundle(request))
    }
}
