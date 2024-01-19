package io.novafoundation.nova.feature_staking_impl.presentation.mappers

import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.feature_account_api.data.model.ChildIdentity
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_account_api.data.model.RootIdentity
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.IdentityModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.IdentityParcelModel

fun mapIdentityToIdentityParcelModel(identity: OnChainIdentity): IdentityParcelModel {
    return with(identity) {
        val childInfo = identity.castOrNull<ChildIdentity>()?.let {
            IdentityParcelModel.ChildInfo(
                parentSeparateDisplay = it.parentIdentity.display,
                childName = it.childName
            )
        }

        IdentityParcelModel(display, legal, web, matrix, email, pgpFingerprint, image, twitter, childInfo)
    }
}

fun mapIdentityParcelModelToIdentity(identity: IdentityParcelModel): OnChainIdentity {
    return with(identity) {
        if (childInfo != null) {
            val parent = RootIdentity(childInfo.parentSeparateDisplay, legal, web, matrix, email, pgpFingerprint, image, twitter)

            ChildIdentity(childInfo.childName, parent)
        } else {
            RootIdentity(display, legal, web, matrix, email, pgpFingerprint, image, twitter)
        }
    }
}

fun mapIdentityParcelModelToIdentityModel(identity: IdentityParcelModel): IdentityModel {
    return with(identity) {
        IdentityModel(display, legal, web, matrix, email, image, twitter)
    }
}
