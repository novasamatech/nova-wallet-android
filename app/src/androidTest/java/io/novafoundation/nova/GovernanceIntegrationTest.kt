package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.firstLoaded
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_api.domain.referendum.filters.ReferendumType
import io.novafoundation.nova.feature_governance_api.domain.referendum.filters.ReferendumTypeFilter
import io.novafoundation.nova.feature_governance_impl.data.RealGovernanceAdditionalState
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.junit.Test
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


class GovernanceIntegrationTest : BaseIntegrationTest() {

    private val accountApi = FeatureUtils.getFeature<AccountFeatureApi>(context, AccountFeatureApi::class.java)
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
        val accountRepository = accountApi.provideAccountRepository()
        val referendaListInteractor = governanceApi.referendaListInteractor
        val updateSystem = governanceApi.governanceUpdateSystem

        updateSystem.start()
            .inBackground()
            .launchIn(this)

        val metaAccount = accountRepository.getSelectedMetaAccount()
        val accountId = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY".toAccountId()

        val chain = chain()
        val selectedGovernance = supportedGovernanceOption(chain, Chain.Governance.V1)

        val filterFlow: Flow<ReferendumTypeFilter> = flow {
            val referendaFilter = ReferendumTypeFilter(ReferendumType.ALL)
            emit(referendaFilter)
        }

        val referendaByGroup = referendaListInteractor.referendaListStateFlow(metaAccount, accountId, selectedGovernance, this, filterFlow).firstLoaded()
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

        val referendumDetails = referendumDetailsInteractor.referendumDetailsFlow(referendumId, selectedGovernance, accountId, CoroutineScope(Dispatchers.Main))
            .first()

        Log.d(this@GovernanceIntegrationTest.LOG_TAG, referendumDetails.toString())

        val callDetails = referendumDetailsInteractor.detailsFor(
            preImage = referendumDetails?.onChainMetadata!!.preImage!!,
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
            governanceOption = supportedGovernanceOption(chain, Chain.Governance.V2),
            scope = this
        )
        Log.d(this@GovernanceIntegrationTest.LOG_TAG, delegates.toString())
    }

    @Test
    fun shouldFetchDelegateDetails() = runTest {
        val delegateAccountId = "DCZyhphXsRLcW84G9WmWEXtAA8DKGtVGSFZLJYty8Ajjyfa".toAccountId() // ChaosDAO

        val interactor = governanceApi.delegateDetailsInteractor
        val updateSystem = governanceApi.governanceUpdateSystem

        updateSystem.start()
            .inBackground()
            .launchIn(this)

        val delegate = interactor.delegateDetailsFlow(delegateAccountId)
        Log.d(this@GovernanceIntegrationTest.LOG_TAG, delegate.toString())
    }

    @Test
    fun shouldFetchChooseTrackData() = runTest {
        val interactor = governanceApi.newDelegationChooseTrackInteractor
        val updateSystem = governanceApi.governanceUpdateSystem

        updateSystem.start()
            .inBackground()
            .launchIn(this)

        val trackData = interactor.observeNewDelegationTrackData().first()

        Log.d(
            this@GovernanceIntegrationTest.LOG_TAG,
            """
                Available: ${trackData.trackPartition.available.size}
                Already voted: ${trackData.trackPartition.alreadyVoted.size}
                Already delegated: ${trackData.trackPartition.alreadyDelegated.size}
                Presets: ${trackData.presets}
            """.trimIndent()
        )
    }

    @Test
    fun shouldFetchDelegators() = runTest {
        val delegateAddress = "DCZyhphXsRLcW84G9WmWEXtAA8DKGtVGSFZLJYty8Ajjyfa" // ChaosDAO

        val interactor = governanceApi.delegateDelegatorsInteractor
        val updateSystem = governanceApi.governanceUpdateSystem

        updateSystem.start()
            .inBackground()
            .launchIn(this)

        val delegators = interactor.delegatorsFlow(delegateAddress.toAccountId()).first()

        Log.d(this@GovernanceIntegrationTest.LOG_TAG, delegators.toString())
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
