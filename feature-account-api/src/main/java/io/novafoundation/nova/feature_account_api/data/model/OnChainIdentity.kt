package io.novafoundation.nova.feature_account_api.data.model

import io.novasama.substrate_sdk_android.runtime.AccountId

interface OnChainIdentity {
    val display: String?
    val legal: String?
    val web: String?
    val matrix: String?
    val email: String?
    val pgpFingerprint: String?
    val image: String?
    val twitter: String?
}

class RootIdentity(
    override val display: String?,
    override val legal: String?,
    override val web: String?,
    override val matrix: String?,
    override val email: String?,
    override val pgpFingerprint: String?,
    override val image: String?,
    override val twitter: String?,
) : OnChainIdentity

class ChildIdentity(
    val childName: String?,
    val parentIdentity: OnChainIdentity,
) : OnChainIdentity by parentIdentity {

    override val display: String = "${parentIdentity.display} / ${childName.orEmpty()}"
}

class SuperOf(
    val parentId: AccountId,
    val childName: String?,
)
