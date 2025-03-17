package io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model

import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

interface DryRunEffects {

    val emittedEvents: List<GenericEvent.Instance>

    val forwardedXcms: ForwardedXcms
}

fun DryRunEffects.usedXcmVersion(): XcmVersion {
    return forwardedXcms.first().first.version
}
