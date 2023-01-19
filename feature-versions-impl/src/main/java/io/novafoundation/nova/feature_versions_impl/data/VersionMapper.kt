package io.novafoundation.nova.feature_versions_impl.data

import io.novafoundation.nova.common.utils.formatting.parseDateISO_8601
import io.novafoundation.nova.feature_versions_api.domain.Severity
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotification

fun mapFromRemoteVersion(versionResponse: VersionResponse, details: String): UpdateNotification {
    return UpdateNotification(
        versionResponse.version,
        details,
        mapSeverity(versionResponse.severity),
        parseDateISO_8601(versionResponse.time)!!
    )
}

fun mapSeverity(severity: String): Severity {
    return when (severity) {
        "Critical" -> Severity.CRITICAL
        else -> Severity.NORMAL
    }
}
