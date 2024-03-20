package io.novafoundation.nova.feature_governance_impl.data.repository.common

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindByteArray
import io.novafoundation.nova.common.data.network.runtime.binding.bindCollectionEnum
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PriorLock
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Proposal
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Tally
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromByteArray
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

fun bindProposal(decoded: Any?, runtimeSnapshot: RuntimeSnapshot): Proposal {
    return when (decoded) {
        is ByteArray -> bindProposalLegacy(decoded)
        is DictEnum.Entry<*> -> bindProposalBound(decoded, runtimeSnapshot)
        else -> incompatible()
    }
}

private fun bindProposalLegacy(decoded: ByteArray): Proposal {
    return Proposal.Legacy(decoded)
}

private fun bindProposalBound(decoded: DictEnum.Entry<*>, runtime: RuntimeSnapshot): Proposal {
    return when (decoded.name) {
        "Legacy" -> {
            val valueAsStruct = decoded.value.castToStruct()
            Proposal.Legacy(bindByteArray(valueAsStruct["hash"]))
        }
        "Inline" -> {
            val bytes = bindByteArray(decoded.value)
            val call = GenericCall.fromByteArray(runtime, bytes)

            Proposal.Inline(bytes, call)
        }
        "Lookup" -> {
            val valueAsStruct = decoded.value.castToStruct()

            Proposal.Lookup(
                hash = bindByteArray(valueAsStruct["hash"]),
                callLength = bindNumber(valueAsStruct["len"])
            )
        }
        else -> incompatible()
    }
}

fun bindTally(decoded: Struct.Instance): Tally {
    return Tally(
        ayes = bindNumber(decoded["ayes"]),
        nays = bindNumber(decoded["nays"]),
        support = bindNumber(decoded["support"] ?: decoded["turnout"])
    )
}

fun bindVoting(decoded: Any): Voting {
    decoded.castToDictEnum()

    return when (decoded.name) {
        "Casting", "Direct" -> {
            val casting = decoded.value.castToStruct()

            val votes = bindVotes(casting["votes"])
            val prior = bindPriorLock(casting["prior"])

            Voting.Casting(votes, prior)
        }

        "Delegating" -> {
            val delegating = decoded.value.castToStruct()

            val balance = bindNumber(delegating["balance"])
            val target = bindAccountId(delegating["target"])
            val conviction = bindConvictionEnum(delegating["conviction"])
            val prior = bindPriorLock(delegating["prior"])

            Voting.Delegating(balance, target, conviction, prior)
        }

        else -> incompatible()
    }
}

fun bindConvictionEnum(decoded: Any?): Conviction {
    return bindCollectionEnum(decoded) { name ->
        when (name) {
            "None" -> Conviction.None
            "Locked1x" -> Conviction.Locked1x
            "Locked2x" -> Conviction.Locked2x
            "Locked3x" -> Conviction.Locked3x
            "Locked4x" -> Conviction.Locked4x
            "Locked5x" -> Conviction.Locked5x
            "Locked6x" -> Conviction.Locked6x
            else -> incompatible()
        }
    }
}

private fun bindVotes(decoded: Any?): Map<ReferendumId, AccountVote> {
    return bindList(decoded) { item ->
        val (referendumId, accountVote) = item.castToList()

        ReferendumId(bindNumber(referendumId)) to bindAccountVote(accountVote)
    }.toMap()
}

private fun bindAccountVote(decoded: Any?): AccountVote {
    decoded.castToDictEnum()

    return when (decoded.name) {
        "Standard" -> {
            val standardVote = decoded.value.castToStruct()

            AccountVote.Standard(
                vote = bindVote(standardVote["vote"]),
                balance = bindNumber(standardVote["balance"])
            )
        }

        "Split" -> {
            val splitVote = decoded.value.castToStruct()

            AccountVote.Split(
                aye = bindNumber(splitVote["aye"]),
                nay = bindNumber(splitVote["nay"])
            )
        }

        "SplitAbstain" -> {
            val splitVote = decoded.value.castToStruct()

            AccountVote.SplitAbstain(
                aye = bindNumber(splitVote["aye"]),
                nay = bindNumber(splitVote["nay"]),
                abstain = bindNumber(splitVote["abstain"])
            )
        }

        else -> AccountVote.Unsupported
    }
}

private fun bindPriorLock(decoded: Any?): PriorLock {
    // 2-tuple
    val (unlockAt, amount) = decoded.castToList()

    return PriorLock(
        unlockAt = bindBlockNumber(unlockAt),
        amount = bindNumber(amount)
    )
}

private fun bindVote(decoded: Any?): Vote {
    return decoded.cast()
}
