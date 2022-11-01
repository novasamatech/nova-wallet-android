package io.novafoundation.nova.feature_governance_impl.data.repository.common

import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PriorLock
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Tally
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct

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
            val prior = bindPriorLock(delegating["prior"])

            Voting.Delegating(balance, prior)
        }

        else -> incompatible()
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

        "Split" -> AccountVote.Split

        else -> incompatible()
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
