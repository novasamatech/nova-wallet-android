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
    operator fun compareTo(other: Version): Int {
        val comparedMajor = major.compareTo(other.major)
        if (comparedMajor != 0) {
            return comparedMajor
        }

        val comparedMinor = minor.compareTo(other.minor)
        if (comparedMinor != 0) {
            return comparedMinor
        }

        return patch.compareTo(other.patch)
    }

    override fun toString(): String {
        return "$major.$minor.$patch"
    }
}

enum class Severity {
    NORMAL, MAJOR, CRITICAL
}
