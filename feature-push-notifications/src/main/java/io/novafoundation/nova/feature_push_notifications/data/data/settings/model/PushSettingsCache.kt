package io.novafoundation.nova.feature_push_notifications.data.data.settings.model

import io.novafoundation.nova.feature_push_notifications.data.domain.model.PushSettings

interface PushSettingsCache {

    val version: String

    fun toPushSettings(): PushSettings
}
