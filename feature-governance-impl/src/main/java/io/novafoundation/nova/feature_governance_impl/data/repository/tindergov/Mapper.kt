package io.novafoundation.nova.feature_governance_impl.data.repository.tindergov

import io.novafoundation.nova.core_db.model.common.ConvictionLocal
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction

fun Conviction.toLocal() = when (this) {
    Conviction.None -> ConvictionLocal.NONE
    Conviction.Locked1x -> ConvictionLocal.LOCKED_1X
    Conviction.Locked2x -> ConvictionLocal.LOCKED_2X
    Conviction.Locked3x -> ConvictionLocal.LOCKED_3X
    Conviction.Locked4x -> ConvictionLocal.LOCKED_4X
    Conviction.Locked5x -> ConvictionLocal.LOCKED_5X
    Conviction.Locked6x -> ConvictionLocal.LOCKED_6X
}

fun ConvictionLocal.toDomain() = when (this) {
    ConvictionLocal.NONE -> Conviction.None
    ConvictionLocal.LOCKED_1X -> Conviction.Locked1x
    ConvictionLocal.LOCKED_2X -> Conviction.Locked2x
    ConvictionLocal.LOCKED_3X -> Conviction.Locked3x
    ConvictionLocal.LOCKED_4X -> Conviction.Locked4x
    ConvictionLocal.LOCKED_5X -> Conviction.Locked5x
    ConvictionLocal.LOCKED_6X -> Conviction.Locked6x
}
