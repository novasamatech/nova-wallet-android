package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChain
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.runBlocking
import org.junit.Test


class GovernanceIntegrationTest : BaseIntegrationTest() {

    private val governanceApi = FeatureUtils.getFeature<GovernanceFeatureApi>(context, GovernanceFeatureApi::class.java)

    @Test
    fun shouldRetrieveOnChainReferenda() = runBlocking<Unit> {
        val chain = chain()
        val onChainReferendaRepository = governanceApi.onChainReferendaRepository

        val referenda = onChainReferendaRepository.getOnChainReferenda(chain.id)

        Log.d(this@GovernanceIntegrationTest.LOG_TAG, referenda.toString())
    }

    @Test
    fun shouldRetrieveConvictionVotes() = runBlocking<Unit> {
        val chain = chain()
        val convictionVotingRepository = governanceApi.convictionVotingRepository

        val accountId = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY".toAccountId()

        val votes = convictionVotingRepository.votingFor(accountId, chain.id)
        Log.d(this@GovernanceIntegrationTest.LOG_TAG, votes.toString())
    }

    @Test
    fun shouldRetrieveTrackLocks() = runBlocking<Unit> {
        val chain = chain()
        val convictionVotingRepository = governanceApi.convictionVotingRepository

        val accountId = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY".toAccountId()

        val trackLocks = convictionVotingRepository.trackLocksFor(accountId, chain.id)
        Log.d(this@GovernanceIntegrationTest.LOG_TAG, trackLocks.toString())
    }

    private suspend fun chain(): Chain = chainRegistry.findChain { it.hasGovernance }!!
}
