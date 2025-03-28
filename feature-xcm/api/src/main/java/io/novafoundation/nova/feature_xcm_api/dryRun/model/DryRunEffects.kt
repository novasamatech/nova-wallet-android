package io.novafoundation.nova.feature_xcm_api.dryRun.model

import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

interface DryRunEffects {

    val emittedEvents: List<GenericEvent.Instance>

    val forwardedXcms: ForwardedXcms
}

fun DryRunEffects.recipientXcmVersion(): XcmVersion {
    // For destination messages, dry run uses recipient's xcm version
    val (_, forwardedMessagesToDestination) = forwardedXcms.first()
    return forwardedMessagesToDestination.first().version
}

fun DryRunEffects.senderXcmVersion(): XcmVersion {
    // For referencing destination, dry run uses sender's xcm version
    val (destination) = forwardedXcms.first()
    return destination.version
}
