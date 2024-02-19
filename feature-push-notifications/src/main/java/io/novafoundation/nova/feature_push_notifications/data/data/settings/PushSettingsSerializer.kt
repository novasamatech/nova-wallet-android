package io.novafoundation.nova.feature_push_notifications.data.data.settings

import com.google.gson.GsonBuilder
import io.novafoundation.nova.common.utils.gson.SealedTypeAdapterFactory
import io.novafoundation.nova.feature_push_notifications.data.data.settings.model.PushSettingsCacheV1
import io.novafoundation.nova.feature_push_notifications.data.domain.model.PushSettings

object PushSettingsSerializer {

    fun gson() = GsonBuilder()
        .registerTypeAdapterFactory(SealedTypeAdapterFactory.of(PushSettingsCacheV1.ChainFeature::class))
        .create()
}
