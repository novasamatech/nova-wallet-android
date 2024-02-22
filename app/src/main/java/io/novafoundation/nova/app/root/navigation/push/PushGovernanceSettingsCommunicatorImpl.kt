package io.novafoundation.nova.app.root.navigation.push

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
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.PushGovernanceSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.PushGovernanceSettingsFragment
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.PushGovernanceSettingsRequester
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.PushGovernanceSettingsResponder

class PushGovernanceSettingsCommunicatorImpl(private val router: PushNotificationsRouter, navigationHolder: NavigationHolder) :
    NavStackInterScreenCommunicator<PushGovernanceSettingsRequester.Request, PushGovernanceSettingsResponder.Response>(navigationHolder),
    PushGovernanceSettingsCommunicator {

    override fun openRequest(request: PushGovernanceSettingsRequester.Request) {
        super.openRequest(request)

        router.openPushGovernanceSettings(PushGovernanceSettingsFragment.getBundle(request))
    }
}
