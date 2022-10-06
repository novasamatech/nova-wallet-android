package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Test


class GovernanceIntegrationTest : BaseIntegrationTest() {

    private val governanceApi = FeatureUtils.getFeature<GovernanceFeatureApi>(context, GovernanceFeatureApi::class.java)

    @Test
    fun shouldRetrieveOnChainReferenda() = runBlocking<Unit> {
        val chain = chain()
        val onChainReferendaRepository = source(chain).referenda

        val referenda = onChainReferendaRepository.getOnChainReferenda(chain.id)

        Log.d(this@GovernanceIntegrationTest.LOG_TAG, referenda.toString())
    }

    @Test
    fun shouldRetrieveConvictionVotes() = runBlocking<Unit> {
        val chain = chain()
        val convictionVotingRepository = source(chain).convictionVoting

        val accountId = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY".toAccountId()

        val votes = convictionVotingRepository.votingFor(accountId, chain.id)
        Log.d(this@GovernanceIntegrationTest.LOG_TAG, votes.toString())
    }

    @Test
    fun shouldRetrieveTrackLocks() = runBlocking<Unit> {
        val chain = chain()
        val convictionVotingRepository = source(chain).convictionVoting

        val accountId = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY".toAccountId()

        val trackLocks = convictionVotingRepository.trackLocksFor(accountId, chain.id)
        Log.d(this@GovernanceIntegrationTest.LOG_TAG, trackLocks.toString())
    }

    @Test
    fun shouldRetrieveReferendaTracks() = runBlocking<Unit> {
        val chain = chain()
        val onChainReferendaRepository = source(chain).referenda

        val tracks = onChainReferendaRepository.getTracks(chain.id)

        Log.d(this@GovernanceIntegrationTest.LOG_TAG, tracks.toString())
    }

    private suspend fun source(chain: Chain) = governanceApi.governanceSourceRegistry.sourceFor(chain.id)

    private suspend fun chain(): Chain = chainRegistry.currentChains.map { chains ->
        chains.find(Chain::hasGovernance)
    }
        .filterNotNull()
        .first()
}
