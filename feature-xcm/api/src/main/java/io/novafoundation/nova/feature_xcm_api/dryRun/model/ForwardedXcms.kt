package io.novafoundation.nova.feature_xcm_api.dryRun.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.feature_xcm_api.message.VersionedRawXcmMessage
import io.novafoundation.nova.feature_xcm_api.message.bindVersionedRawXcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.bindVersionedMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.VersionedXcmLocation

typealias ForwardedXcms = List<Pair<VersionedXcmLocation, List<VersionedRawXcmMessage>>>

fun bindForwardedXcms(decodedInstance: Any?): ForwardedXcms {
    return bindList(decodedInstance) { inner ->
        val (locationRaw, messagesRaw) = inner.castToList()
        val messages = bindList(messagesRaw, ::bindVersionedRawXcmMessage)
        val location = bindVersionedMultiLocation(locationRaw)

        location to messages
    }
}
fun ForwardedXcms.getByLocation(location: VersionedXcmLocation): List<VersionedRawXcmMessage> {
    return find { it.first == location }?.second.orEmpty()
}
