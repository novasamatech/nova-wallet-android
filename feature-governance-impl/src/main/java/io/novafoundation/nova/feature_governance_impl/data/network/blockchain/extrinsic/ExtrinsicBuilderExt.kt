package io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.argumentType
import io.novafoundation.nova.common.utils.democracy
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.extrinsic.multi.CallBuilder
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.util.constructAccountLookupInstance
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.instances.AddressInstanceConstructor
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.call

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

fun CallBuilder.convictionVotingVote(
    referendumId: ReferendumId,
    vote: AccountVote
): CallBuilder {
    return addCall(
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

fun CallBuilder.democracyVote(
    referendumId: ReferendumId,
    vote: AccountVote
): CallBuilder {
    return addCall(
        moduleName = Modules.DEMOCRACY,
        callName = "vote",
        arguments = mapOf(
            "ref_index" to referendumId.value,
            "vote" to vote.prepareForEncoding()
        )
    )
}

fun ExtrinsicBuilder.democracyUnlock(accountId: AccountId): ExtrinsicBuilder {
    val accountLookupType = runtime.metadata.democracy().call("unlock").argumentType("target")

    return call(
        moduleName = Modules.DEMOCRACY,
        callName = "unlock",
        arguments = mapOf(
            "target" to accountLookupType.constructAccountLookupInstance(accountId)
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

fun CallBuilder.convictionVotingDelegate(
    delegate: AccountId,
    trackId: TrackId,
    amount: Balance,
    conviction: Conviction
): CallBuilder {
    return addCall(
        moduleName = Modules.CONVICTION_VOTING,
        callName = "delegate",
        arguments = mapOf(
            "class" to trackId.value,
            "to" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, delegate),
            "conviction" to conviction.prepareForEncoding(),
            "balance" to amount
        )
    )
}

fun CallBuilder.convictionVotingUndelegate(trackId: TrackId): CallBuilder {
    return addCall(
        moduleName = Modules.CONVICTION_VOTING,
        callName = "undelegate",
        arguments = mapOf(
            "class" to trackId.value,
        )
    )
}

private fun Conviction.prepareForEncoding(): Any {
    return DictEnum.Entry(name, null)
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

        is AccountVote.SplitAbstain -> DictEnum.Entry(
            name = "SplitAbstain",
            value = structOf(
                "aye" to this.aye,
                "nay" to this.nay,
                "abstain" to this.abstain
            )
        )

        else -> error("Not supported yet")
    }
}
