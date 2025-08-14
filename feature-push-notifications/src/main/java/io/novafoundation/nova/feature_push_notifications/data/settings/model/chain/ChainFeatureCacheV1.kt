package io.novafoundation.nova.feature_push_notifications.data.settings.model.chain

import io.novafoundation.nova.feature_push_notifications.domain.model.PushSettings
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

sealed class ChainFeatureCacheV1 {

    object All : ChainFeatureCacheV1()

    data class Concrete(val chainIds: List<ChainId>) : ChainFeatureCacheV1()
}

fun ChainFeatureCacheV1.toDomain(): PushSettings.ChainFeature {
    return when (this) {
        is ChainFeatureCacheV1.All -> PushSettings.ChainFeature.All
        is ChainFeatureCacheV1.Concrete -> PushSettings.ChainFeature.Concrete(chainIds)
    }
}
