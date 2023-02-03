package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model

import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackModel
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class DelegateListModel(
    val icon: DelegateIcon,
    val accountId: AccountId,
    val name: String,
    val type: DelegateTypeModel?,
    val description: String?,
    val stats: DelegateStatsModel,
    val delegation: YourDelegationInfo?
) {

    class YourDelegationInfo(
        val firstTrack: TrackModel,
        val otherTracksCount: String?,
        val votes: Votes?,
    )

    class Votes(val amount: String, val details: String)
}
