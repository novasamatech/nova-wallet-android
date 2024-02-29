package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model

import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VoteModel
import io.novasama.substrate_sdk_android.runtime.AccountId

class DelegateListModel(
    val icon: DelegateIcon,
    val accountId: AccountId,
    val name: String,
    val type: DelegateTypeModel?,
    val description: String?,
    val stats: DelegateStatsModel?,
    val delegation: YourDelegationInfo?
) {

    data class YourDelegationInfo(
        val firstTrack: TrackModel,
        val otherTracksCount: String?,
        val votes: VoteModel?,
    )
}
