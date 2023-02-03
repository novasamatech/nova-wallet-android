package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.removeVotes

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.submitExtrinsicWithSelectedWalletAndWaitBlockInclusion
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votedReferenda
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.removeVotes.RemoveTrackVotesInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.state.selectedOption
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealRemoveTrackVotesInteractor(
    private val extrinsicService: ExtrinsicService,
    private val governanceSharedState: GovernanceSharedState,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val accountRepository: AccountRepository,
) : RemoveTrackVotesInteractor {

    override suspend fun calculateFee(trackIds: Collection<TrackId>): Balance = withContext(Dispatchers.IO) {
        val (chain, governance) = useSelectedGovernance()
        val accountId = accountRepository.requireIdOfSelectedMetaAccountIn(chain)

        extrinsicService.estimateFee(chain) {
            governance.removeVotes(trackIds, extrinsicBuilder = this, chain.id, accountId)
        }
    }

    override suspend fun removeTrackVotes(trackIds: Collection<TrackId>): Result<ExtrinsicStatus.InBlock> = withContext(Dispatchers.IO) {
        val (chain, governance) = useSelectedGovernance()

        extrinsicService.submitExtrinsicWithSelectedWalletAndWaitBlockInclusion(chain) { origin ->
            governance.removeVotes(trackIds, extrinsicBuilder = this, chain.id, origin)
        }
    }

    private suspend fun GovernanceSource.removeVotes(
        trackIds: Collection<TrackId>,
        extrinsicBuilder: ExtrinsicBuilder,
        chainId: ChainId,
        accountId: AccountId
    ) {
        val votings = convictionVoting.votingFor(accountId, chainId, trackIds)

        votings.entries.onEach { (trackId, voting) ->
            voting.votedReferenda().onEach { referendumId ->
                with(convictionVoting) {
                    extrinsicBuilder.removeVote(trackId, referendumId)
                }
            }
        }
    }

    private suspend fun useSelectedGovernance(): Pair<Chain, GovernanceSource> {
        val option = governanceSharedState.selectedOption()
        val source = governanceSourceRegistry.sourceFor(option)
        val chain = option.assetWithChain.chain

        return chain to source
    }
}
