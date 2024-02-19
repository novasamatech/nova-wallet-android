package io.novafoundation.nova.feature_push_notifications.data.data.settings.model

class VersionedPushSettingsCache(
    val version: String,
    val settings: String // json here
)
