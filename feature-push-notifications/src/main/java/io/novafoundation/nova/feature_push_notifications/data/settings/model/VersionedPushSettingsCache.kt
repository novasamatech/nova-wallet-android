package io.novafoundation.nova.feature_push_notifications.data.settings.model

typealias Json = String

class VersionedPushSettingsCache(
    val version: String,
    val settings: Json
)
