package io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation

import io.novasama.substrate_sdk_android.runtime.AccountId

class DelegateMetadata(
    val accountId: AccountId,
    val shortDescription: String,
    val longDescription: String?,
    val profileImageUrl: String?,
    val isOrganization: Boolean,
    val name: String,
)
