package io.novafoundation.nova.feature_push_notifications.data.settings

import com.google.gson.GsonBuilder
import io.novafoundation.nova.common.utils.gson.SealedTypeAdapterFactory
import io.novafoundation.nova.feature_push_notifications.data.settings.model.PushSettingsCacheV1

object PushSettingsSerializer {

    fun gson() = GsonBuilder()
        .registerTypeAdapterFactory(SealedTypeAdapterFactory.of(PushSettingsCacheV1.ChainFeature::class))
        .create()
}
