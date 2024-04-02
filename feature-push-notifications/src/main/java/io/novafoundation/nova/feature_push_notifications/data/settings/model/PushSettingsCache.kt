package io.novafoundation.nova.feature_push_notifications.data.settings.model

import io.novafoundation.nova.feature_push_notifications.domain.model.PushSettings

interface PushSettingsCache {

    val version: String

    fun toPushSettings(): PushSettings
}
