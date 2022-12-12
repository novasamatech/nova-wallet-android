package io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

fun ExtrinsicBuilder.convictionVotingVote(
    referendumId: ReferendumId,
    vote: AccountVote
): ExtrinsicBuilder {
    return call(
        moduleName = Modules.CONVICTION_VOTING,
        callName = "vote",
        arguments = mapOf(
            "poll_index" to referendumId.value,
            "vote" to vote.prepareForEncoding()
        )
    )
}

fun ExtrinsicBuilder.convictionVotingUnlock(
    trackId: TrackId,
    accountId: AccountId
): ExtrinsicBuilder {
    return call(
        moduleName = Modules.CONVICTION_VOTING,
        callName = "unlock",
        arguments = mapOf(
            "class" to trackId.value,
            "target" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, accountId)
        )
    )
}

fun ExtrinsicBuilder.convictionVotingRemoveVote(
    trackId: TrackId,
    referendumId: ReferendumId,
): ExtrinsicBuilder {
    return call(
        moduleName = Modules.CONVICTION_VOTING,
        callName = "remove_vote",
        arguments = mapOf(
            "class" to trackId.value,
            "index" to referendumId.value
        )
    )
}

fun ExtrinsicBuilder.democracyVote(
    referendumId: ReferendumId,
    vote: AccountVote
): ExtrinsicBuilder {
    return call(
        moduleName = Modules.DEMOCRACY,
        callName = "vote",
        arguments = mapOf(
            "ref_index" to referendumId.value,
            "vote" to vote.prepareForEncoding()
        )
    )
}

fun ExtrinsicBuilder.democracyUnlock(accountId: AccountId): ExtrinsicBuilder {
    return call(
        moduleName = Modules.DEMOCRACY,
        callName = "unlock",
        arguments = mapOf(
            "target" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, accountId)
        )
    )
}

fun ExtrinsicBuilder.democracyRemoveVote(
    referendumId: ReferendumId,
): ExtrinsicBuilder {
    return call(
        moduleName = Modules.DEMOCRACY,
        callName = "remove_vote",
        arguments = mapOf(
            "index" to referendumId.value
        )
    )
}

private fun AccountVote.prepareForEncoding(): Any {
    return when (this) {
        AccountVote.Unsupported -> error("Not yet supported")

        is AccountVote.Standard -> DictEnum.Entry(
            name = "Standard",
            value = structOf(
                "vote" to vote,
                "balance" to balance
            )
        )
    }
}
