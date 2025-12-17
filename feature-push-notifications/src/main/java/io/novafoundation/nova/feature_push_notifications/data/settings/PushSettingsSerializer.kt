package io.novafoundation.nova.feature_push_notifications.data.settings

import com.google.gson.GsonBuilder
import io.novafoundation.nova.common.utils.gson.SealedTypeAdapterFactory
import io.novafoundation.nova.feature_push_notifications.data.settings.model.chain.ChainFeatureCacheV1

object PushSettingsSerializer {

    fun gson() = GsonBuilder()
        .registerTypeAdapterFactory(SealedTypeAdapterFactory.of(ChainFeatureCacheV1::class))
        .create()
}
