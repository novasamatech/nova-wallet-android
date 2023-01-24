package io.novafoundation.nova.feature_versions_api.domain

import java.util.Date

class UpdateNotification(
    val version: Version,
    val changelog: String,
    val severity: Severity,
    val time: Date
)

class Version(
    private val major: Long,
    private val minor: Long,
    private val patch: Long
) {

    companion object {
        fun getComparator(): Comparator<Version> {
            return compareBy<Version> { it.major }
                .thenBy { it.minor }
                .thenBy { it.patch }
        }
    }

    operator fun compareTo(other: Version): Int {
        return getComparator()
            .compare(this, other)
    }

    override fun toString(): String {
        return "$major.$minor.$patch"
    }
}

enum class Severity {
    NORMAL, MAJOR, CRITICAL
}
