package io.novafoundation.nova.feature_governance_impl.data.repository.tindergov

import io.novafoundation.nova.core_db.model.common.ConvictionLocal
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction

fun Conviction.toLocal() = when (this) {
    Conviction.None -> ConvictionLocal.None
    Conviction.Locked1x -> ConvictionLocal.LOCKED_1x
    Conviction.Locked2x -> ConvictionLocal.LOCKED_2x
    Conviction.Locked3x -> ConvictionLocal.LOCKED_3x
    Conviction.Locked4x -> ConvictionLocal.LOCKED_4x
    Conviction.Locked5x -> ConvictionLocal.LOCKED_5x
    Conviction.Locked6x -> ConvictionLocal.LOCKED_6X
}

fun ConvictionLocal.toDomain() = when (this) {
    ConvictionLocal.None -> Conviction.None
    ConvictionLocal.LOCKED_1x -> Conviction.Locked1x
    ConvictionLocal.LOCKED_2x -> Conviction.Locked2x
    ConvictionLocal.LOCKED_3x -> Conviction.Locked3x
    ConvictionLocal.LOCKED_4x -> Conviction.Locked4x
    ConvictionLocal.LOCKED_5x -> Conviction.Locked5x
    ConvictionLocal.LOCKED_6X -> Conviction.Locked6x
}
