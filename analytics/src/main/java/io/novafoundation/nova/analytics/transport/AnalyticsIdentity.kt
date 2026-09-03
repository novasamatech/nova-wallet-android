package io.novafoundation.nova.analytics.transport

import io.novafoundation.nova.common.data.storage.Preferences
import java.util.UUID

private const val PREFS_INSTALL_ID = "analytics_install_id"

interface AnalyticsIdentity {

    val sessionId: String

    fun installId(): String

    fun resetInstallId()
}

class RealAnalyticsIdentity(
    private val preferences: Preferences
) : AnalyticsIdentity {

    override val sessionId: String = UUID.randomUUID().toString()

    override fun installId(): String {
        preferences.getString(PREFS_INSTALL_ID)?.let { return it }

        val generated = UUID.randomUUID().toString()
        preferences.putString(PREFS_INSTALL_ID, generated)
        return generated
    }

    override fun resetInstallId() = preferences.putString(PREFS_INSTALL_ID, UUID.randomUUID().toString())
}
