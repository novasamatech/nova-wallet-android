package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.metadata.response

class DelegateMetadataRemote(
    val address: String,
    val name: String,
    val image: String,
    val shortDescription: String,
    val longDescription: String?,
    val isOrganization: Boolean
)
