package io.novafoundation.nova.feature_external_sign_impl

import io.novafoundation.nova.common.navigation.ReturnableRouter

interface ExternalSignRouter : ReturnableRouter {

    fun openExtrinsicDetails(extrinsicContent: String)
}
