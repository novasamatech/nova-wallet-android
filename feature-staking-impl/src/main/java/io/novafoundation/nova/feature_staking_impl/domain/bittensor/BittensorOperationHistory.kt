package io.novafoundation.nova.feature_staking_impl.domain.bittensor

data class BittensorOperationHistory(
    val operations: List<BittensorReportedOperation>,
    val allProviderPagesRead: Boolean
)
