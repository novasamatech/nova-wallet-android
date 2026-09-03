package io.novafoundation.nova.feature_governance_impl.presentation.common.analytics

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction

fun Conviction.toAnalyticsConvictionLevel(): String {
    return when (this) {
        Conviction.None -> "0.1x"
        Conviction.Locked1x -> "1x"
        Conviction.Locked2x -> "2x"
        Conviction.Locked3x -> "3x"
        Conviction.Locked4x -> "4x"
        Conviction.Locked5x -> "5x"
        Conviction.Locked6x -> "6x"
    }
}

fun VoteType.toAnalyticsVoteDirection(): String {
    return when (this) {
        VoteType.AYE -> "aye"
        VoteType.NAY -> "nay"
        VoteType.ABSTAIN -> "abstain"
    }
}
