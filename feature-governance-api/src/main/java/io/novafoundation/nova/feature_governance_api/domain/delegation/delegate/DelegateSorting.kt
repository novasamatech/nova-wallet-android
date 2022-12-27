package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate

enum class DelegateSorting {
    DELEGATIONS, DELEGATED_VOTES, VOTING_ACTIVITY
}

fun DelegateSorting.delegateComparator(): Comparator<Delegate> {
    return when(this) {
        DelegateSorting.DELEGATIONS -> compareByDescending { it.stats.delegationsCount }
        DelegateSorting.DELEGATED_VOTES -> compareByDescending { it.stats.delegatedVotes }
        DelegateSorting.VOTING_ACTIVITY -> compareByDescending { it.stats.recentVotes.numberOfVotes }
    }
}
