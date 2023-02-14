package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model

data class DelegateStatsModel(
    val delegations: String,
    val delegatedVotes: String,
    val recentVotes: RecentVotes
)

data class RecentVotes(val label: String, val value: String)
