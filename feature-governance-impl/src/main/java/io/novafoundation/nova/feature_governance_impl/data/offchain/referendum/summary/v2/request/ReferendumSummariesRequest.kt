package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.request

class ReferendumSummariesRequest(
    val chainId: String,
    val languageIsoCode: String,
    val referendumIds: List<String>
)
