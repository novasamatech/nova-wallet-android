package io.novafoundation.nova.feature_push_notifications.data.data.settings

import com.google.gson.GsonBuilder
import io.novafoundation.nova.common.utils.gson.SealedTypeAdapterFactory

object PushSettingsSerializer {

    fun gson() = GsonBuilder()
        .registerTypeAdapterFactory(SealedTypeAdapterFactory.of(PushSettings.ChainFeature::class))
        .create()
}
