package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.request

class ReferendumSummaryRequest(
    val chainId: String,
    val languageIsoCode: String,
    val referendumId: String
)
