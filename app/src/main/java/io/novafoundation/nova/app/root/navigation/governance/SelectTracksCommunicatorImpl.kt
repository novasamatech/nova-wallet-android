package io.novafoundation.nova.app.root.navigation.governance

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsRequester
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsResponder
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksRequester
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksResponder
import io.novafoundation.nova.feature_account_impl.presentation.account.list.multipleSelecting.SelectMultipleWalletsFragment
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.governanceTracks.SelectGovernanceTracksFragment

class SelectTracksCommunicatorImpl(private val router: GovernanceRouter, navigationHolder: NavigationHolder) :
    NavStackInterScreenCommunicator<SelectTracksRequester.Request, SelectTracksResponder.Response>(navigationHolder),
    SelectTracksCommunicator {

    override fun openRequest(request: SelectTracksRequester.Request) {
        super.openRequest(request)

        router.openSelectGovernanceTracks(SelectGovernanceTracksFragment.getBundle(request))
    }
}
