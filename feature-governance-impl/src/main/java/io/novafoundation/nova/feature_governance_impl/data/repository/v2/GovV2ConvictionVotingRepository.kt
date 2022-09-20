package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.utils.convictionVoting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PriorLock
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import java.math.BigInteger

class GovV2ConvictionVotingRepository(
    private val remoteStorageSource: StorageDataSource,
) : ConvictionVotingRepository {

    override suspend fun trackLocksFor(accountId: AccountId, chainId: ChainId): Map<TrackId, Balance> {
        return remoteStorageSource.query(chainId) {
            runtime.metadata.convictionVoting().storage("ClassLocksFor").query(accountId, binding = ::bindTrackLocks)
                .toMap()
        }
    }

    override suspend fun votingFor(accountId: AccountId, chainId: ChainId): Map<TrackId, Voting> {
        return remoteStorageSource.query(chainId) {
            runtime.metadata.convictionVoting().storage("VotingFor").entries(
                accountId,
                keyExtractor = { (_: AccountId, trackId: BigInteger) -> TrackId(trackId) },
                binding = { decoded, _ -> bindVoting(decoded) }
            )
        }
    }

    private fun bindTrackLocks(decoded: Any?): List<Pair<TrackId, Balance>> {
        return bindList(decoded) { item ->
            val (trackId, balance) = item.castToList()

            TrackId(bindNumber(trackId)) to bindNumber(balance)
        }
    }

    private fun bindVoting(decoded: Any?): Voting {
        decoded.castToDictEnum()

        return when(decoded.name) {
            "Casting" -> {
                val casting = decoded.value.castToStruct()

                val votes = bindVotes(casting["votes"])
                val prior = bindPriorLock(casting["prior"])

                Voting.Casting(votes, prior)
            }

            "Delegating" -> Voting.Delegating

            else -> incompatible()
        }
    }

    private fun bindVotes(decoded: Any?): Map<ReferendumId, AccountVote> {
        return bindList(decoded) { item ->
            val (trackId, accountVote) = item.castToList()

            ReferendumId(bindNumber(trackId)) to bindAccountVote(accountVote)
        }.toMap()
    }

    private fun bindAccountVote(decoded: Any?): AccountVote {
        decoded.castToDictEnum()

        return when(decoded.name) {
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
}
