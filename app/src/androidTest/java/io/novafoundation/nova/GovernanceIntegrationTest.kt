package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.childScope
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigInteger


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

    @Test
    fun shouldRetrieveDomainReferendaPreviews() = runBlocking<Unit> {
        val childScope = childScope()

        val referendaListInteractor = governanceApi.referendaListInteractor
        val updateSystem = governanceApi.governanceUpdateSystem

        updateSystem.start()
            .inBackground()
            .launchIn(childScope)

        val accountId = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY".toAccountId()

        val referendaByGroup = referendaListInteractor.referendaFlow(accountId, chain()).first()
        val referenda = referendaByGroup.values.flatten()

        Log.d(this@GovernanceIntegrationTest.LOG_TAG,referenda.joinToString("\n"))

        childScope.cancel()
    }

    @Test
    fun shouldRetrieveDomainReferendumDetails() = runBlocking<Unit> {
        val childScope = childScope()

        val referendumDetailsInteractor = governanceApi.referendumDetailsInteractor
        val updateSystem = governanceApi.governanceUpdateSystem

        updateSystem.start()
            .inBackground()
            .launchIn(childScope)

        val accountId = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY".toAccountId()
        val referendumId = ReferendumId(BigInteger.ZERO)
        val chain = chain()

        val referendumDetails = referendumDetailsInteractor.referendumDetailsFlow(referendumId, chain, accountId)
            .first()

        Log.d(this@GovernanceIntegrationTest.LOG_TAG, referendumDetails.toString())

        val callDetails = referendumDetailsInteractor.detailsFor(
            preImage = referendumDetails.onChainMetadata!!.preImage!!,
            chain = chain
        )

        Log.d(this@GovernanceIntegrationTest.LOG_TAG, callDetails.toString())

        childScope.cancel()
    }

    @Test
    fun shouldRetrieveVoters() = runBlocking<Unit> {
        val interactor = governanceApi.referendumVotersInteractor

        val referendumId = ReferendumId(BigInteger.ZERO)
        val chain = chain()

        val referendumVoters = interactor.votersFlow(referendumId, chain, VoteType.AYE)
            .first()

        Log.d(this@GovernanceIntegrationTest.LOG_TAG, referendumVoters.toString())
    }

    private suspend fun source(chain: Chain) = governanceApi.governanceSourceRegistry.sourceFor(chain.id)

    private suspend fun chain(): Chain = chainRegistry.currentChains.map { chains ->
        chains.find(Chain::hasGovernance)
    }
        .filterNotNull()
        .first()
}
