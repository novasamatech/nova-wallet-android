package io.novafoundation.nova.feature_account_api.presenatation.mixin.identity

import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity

class IdentityModel(
    val display: String?,
    val legal: String?,
    val web: String?,
    val riot: String?,
    val email: String?,
    val image: String?,
    val twitter: String?
) {

    companion object;
}

fun IdentityModel.Companion.from(identity: OnChainIdentity): IdentityModel {
    return identity.run {
        IdentityModel(
            display = display,
            legal = legal,
            web = web,
            riot = riot,
            email = email,
            image = image,
            twitter = twitter
        )
    }
}
