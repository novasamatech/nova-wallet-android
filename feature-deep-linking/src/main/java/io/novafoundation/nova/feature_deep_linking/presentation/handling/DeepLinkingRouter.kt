package io.novafoundation.nova.feature_deep_linking.presentation.handling

import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

interface DeepLinkingRouter {

    fun openAssetDetails(payload: AssetPayload)

    fun openDAppBrowser(url: String)

    fun openImportAccountScreen(payload: ImportAccountPayload)

    fun openReferendum(payload: ReferendumDetailsPayload)

    fun openStakingDashboard()

    fun openAccountMigrationPairing(scheme: String)
}
