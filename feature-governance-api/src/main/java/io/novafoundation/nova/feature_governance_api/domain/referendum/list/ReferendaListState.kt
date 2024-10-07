package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class ReferendaListState(
    val groupedReferenda: GroupedList<ReferendumGroup, ReferendumPreview>,
    val locksOverview: GovernanceLocksOverview?,
    val delegated: DelegatedState,
    val availableToVoteReferenda: List<ReferendumPreview>
)

class GovernanceLocksOverview(
    val locked: Balance,
    val hasClaimableLocks: Boolean
)

sealed class DelegatedState {

    object DelegationNotSupported : DelegatedState()

    object NotDelegated : DelegatedState()

    class Delegated(val amount: Balance) : DelegatedState()
}
