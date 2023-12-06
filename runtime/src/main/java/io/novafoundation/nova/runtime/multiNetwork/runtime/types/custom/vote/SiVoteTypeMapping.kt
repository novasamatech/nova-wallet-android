package io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote

import jp.co.soramitsu.fearless_utils.runtime.definitions.v14.typeMapping.ReplaceTypesSiTypeMapping

fun SiVoteTypeMapping(): ReplaceTypesSiTypeMapping {
    val voteType = VoteType("NovaWallet.ConvictionVote")

    return ReplaceTypesSiTypeMapping(
        "pallet_democracy.vote.Vote" to voteType,
        "pallet_conviction_voting.vote.Vote" to voteType
    )
}
