package io.novafoundation.nova.feature_versions_impl.data

import io.novafoundation.nova.common.utils.formatting.parseDateISO_8601_NoMs
import io.novafoundation.nova.feature_versions_api.domain.Severity
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotification
import io.novafoundation.nova.feature_versions_api.domain.Version

const val REMOTE_SEVERITY_CRITICAL = "Critical"
const val REMOTE_SEVERITY_MAJOR = "Major"
const val REMOTE_SEVERITY_NORMAL = "Normal"

fun mapFromRemoteVersion(version: Version, versionResponse: VersionResponse, changelog: String?): UpdateNotification {
    return UpdateNotification(
        version,
        changelog,
        mapSeverity(versionResponse.severity),
        parseDateISO_8601_NoMs(versionResponse.time)!!
    )
}

fun mapSeverity(severity: String): Severity {
    return when (severity) {
        REMOTE_SEVERITY_CRITICAL -> Severity.CRITICAL
        REMOTE_SEVERITY_MAJOR -> Severity.MAJOR
        else -> Severity.NORMAL
    }
}
