package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model

enum class DelegateSorting {
    DELEGATIONS, DELEGATED_VOTES, VOTING_ACTIVITY
}

fun DelegateSorting.delegateComparator(): Comparator<DelegatePreview> {
    return when (this) {
        DelegateSorting.DELEGATIONS -> compareByDescending { it.stats?.delegationsCount }
        DelegateSorting.DELEGATED_VOTES -> compareByDescending { it.stats?.delegatedVotes }
        DelegateSorting.VOTING_ACTIVITY -> compareByDescending { it.stats?.recentVotes }
    }
}
