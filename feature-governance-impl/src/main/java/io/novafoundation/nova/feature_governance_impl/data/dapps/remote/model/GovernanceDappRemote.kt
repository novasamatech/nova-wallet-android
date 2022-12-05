package io.novafoundation.nova.feature_governance_impl.data.dapps.remote.model

class GovernanceChainDappsRemote(
    val chainId: String,
    val dapps: List<GovernanceDappRemote>
)

class GovernanceDappRemote(
    val title: String,
    val urlV1: String?,
    val urlV2: String?,
    val icon: String,
    val details: String
)
