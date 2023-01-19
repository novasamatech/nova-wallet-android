package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateFiltering
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateSorting
import io.novafoundation.nova.feature_governance_impl.data.RealGovernanceAdditionalState
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.junit.Test
import java.math.BigInteger


class GovernanceIntegrationTest : BaseIntegrationTest() {

    private val governanceApi = FeatureUtils.getFeature<GovernanceFeatureApi>(context, GovernanceFeatureApi::class.java)

    @Test
    fun shouldRetrieveOnChainReferenda() = runTest {
        val chain = chain()
        val selectedGovernance = supportedGovernanceOption(chain, Chain.Governance.V1)

        val onChainReferendaRepository = source(selectedGovernance).referenda

        val referenda = onChainReferendaRepository.getAllOnChainReferenda(chain.id)

        Log.d(this@GovernanceIntegrationTest.LOG_TAG, referenda.toString())
    }

    @Test
    fun shouldRetrieveConvictionVotes() = runTest {
        val chain = chain()
        val selectedGovernance = supportedGovernanceOption(chain, Chain.Governance.V1)

        val convictionVotingRepository = source(selectedGovernance).convictionVoting

        val accountId = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY".toAccountId()

        val votes = convictionVotingRepository.votingFor(accountId, chain.id)
        Log.d(this@GovernanceIntegrationTest.LOG_TAG, votes.toString())
    }

    @Test
    fun shouldRetrieveTrackLocks() = runTest {
        val chain = chain()
        val selectedGovernance = supportedGovernanceOption(chain, Chain.Governance.V1)

        val convictionVotingRepository = source(selectedGovernance).convictionVoting

        val accountId = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY".toAccountId()

        val fullChainAssetId = FullChainAssetId(chain.id, chain.utilityAsset.id)

        val trackLocks = convictionVotingRepository.trackLocksFlow(accountId, fullChainAssetId).first()
        Log.d(this@GovernanceIntegrationTest.LOG_TAG, trackLocks.toString())
    }

    @Test
    fun shouldRetrieveReferendaTracks() = runTest {
        val chain = chain()
        val selectedGovernance = supportedGovernanceOption(chain, Chain.Governance.V1)

        val onChainReferendaRepository = source(selectedGovernance).referenda

        val tracks = onChainReferendaRepository.getTracks(chain.id)

        Log.d(this@GovernanceIntegrationTest.LOG_TAG, tracks.toString())
    }

    @Test
    fun shouldRetrieveDomainReferendaPreviews() = runTest {
        val referendaListInteractor = governanceApi.referendaListInteractor
        val updateSystem = governanceApi.governanceUpdateSystem

        updateSystem.start()
            .inBackground()
            .launchIn(this)

        val accountId = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY".toAccountId()

        val chain = chain()
        val selectedGovernance = supportedGovernanceOption(chain, Chain.Governance.V1)

        val referendaByGroup = referendaListInteractor.referendaListStateFlow(accountId, selectedGovernance).first()
        val referenda = referendaByGroup.groupedReferenda.values.flatten()

        Log.d(this@GovernanceIntegrationTest.LOG_TAG, referenda.joinToString("\n"))
    }

    @Test
    fun shouldRetrieveDomainReferendumDetails() = runTest {
        val referendumDetailsInteractor = governanceApi.referendumDetailsInteractor
        val updateSystem = governanceApi.governanceUpdateSystem

        updateSystem.start()
            .inBackground()
            .launchIn(this)

        val accountId = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY".toAccountId()
        val referendumId = ReferendumId(BigInteger.ZERO)
        val chain = chain()
        val selectedGovernance = supportedGovernanceOption(chain, Chain.Governance.V1)

        val referendumDetails = referendumDetailsInteractor.referendumDetailsFlow(referendumId, selectedGovernance, accountId)
            .first()

        Log.d(this@GovernanceIntegrationTest.LOG_TAG, referendumDetails.toString())

        val callDetails = referendumDetailsInteractor.detailsFor(
            preImage = referendumDetails.onChainMetadata!!.preImage!!,
            chain = chain
        )

        Log.d(this@GovernanceIntegrationTest.LOG_TAG, callDetails.toString())
    }

    @Test
    fun shouldRetrieveVoters() = runTest {
        val interactor = governanceApi.referendumVotersInteractor

        val referendumId = ReferendumId(BigInteger.ZERO)
        val referendumVoters = interactor.votersFlow(referendumId, VoteType.AYE)
            .first()

        Log.d(this@GovernanceIntegrationTest.LOG_TAG, referendumVoters.toString())
    }

    @Test
    fun shouldFetchDelegatesList() = runTest {
        val interactor = governanceApi.delegateListInteractor
        val updateSystem = governanceApi.governanceUpdateSystem

        updateSystem.start()
            .inBackground()
            .launchIn(this)

        val chain = kusama()
        val delegates = interactor.getDelegates(
            sorting = DelegateSorting.DELEGATIONS,
            filtering = DelegateFiltering.ALL_ACCOUNTS,
            governanceOption = supportedGovernanceOption(chain, Chain.Governance.V2)
        )
        Log.d(this@GovernanceIntegrationTest.LOG_TAG, delegates.toString())
    }

    @Test
    fun shouldFetchDelegateDetails() = runTest {
        val delegateAddress = "DCZyhphXsRLcW84G9WmWEXtAA8DKGtVGSFZLJYty8Ajjyfa" // ChaosDAO

        val interactor = governanceApi.delegateDetailsInteractor
        val updateSystem = governanceApi.governanceUpdateSystem

        updateSystem.start()
            .inBackground()
            .launchIn(this)

        val chain = kusama()
        val delegate = interactor.getDelegateDetails(
            delegateAddress = delegateAddress,
            governanceOption = supportedGovernanceOption(chain, Chain.Governance.V2)
        )
        Log.d(this@GovernanceIntegrationTest.LOG_TAG, delegate.toString())
    }

    private suspend fun source(supportedGovernance: SupportedGovernanceOption) = governanceApi.governanceSourceRegistry.sourceFor(supportedGovernance)

    private fun supportedGovernanceOption(chain: Chain, governance: Chain.Governance) =
        SupportedGovernanceOption(
            ChainWithAsset(chain, chain.utilityAsset),
            RealGovernanceAdditionalState(
                governance,
                false
            )
        )

    private suspend fun chain(): Chain = chainRegistry.currentChains.map { chains ->
        chains.find { it.governance.isNotEmpty() }
    }
        .filterNotNull()
        .first()

    private suspend fun kusama(): Chain = chainRegistry.currentChains.mapNotNull { chains ->
        chains.find { it.externalApi<Chain.ExternalApi.GovernanceDelegations>() != null }
    }.first()
}
