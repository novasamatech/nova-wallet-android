package io.novafoundation.nova.feature_crowdloan_impl.domain.main

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.mapFundInfoToCrowdloan
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

typealias GroupedCrowdloans = GroupedList<KClass<out Crowdloan.State>, Crowdloan>

class CrowdloanInteractor(
    private val crowdloanRepository: CrowdloanRepository,
    private val chainStateRepository: ChainStateRepository,
    private val contributionsRepository: ContributionsRepository
) {

    fun groupedCrowdloansFlow(chain: Chain, account: MetaAccount): Flow<GroupedCrowdloans> {
        return crowdloansFlow(chain, account)
            .map { groupCrowdloans(it) }
    }

    private fun crowdloansFlow(chain: Chain, account: MetaAccount): Flow<List<Crowdloan>> {
        return flow {
            val accountId = account.accountIdIn(chain)

            emitAll(crowdloanListFlow(chain, accountId))
        }
    }

    private fun groupCrowdloans(crowdloans: List<Crowdloan>): GroupedCrowdloans {
        return crowdloans.groupBy { it.state::class }
            .toSortedMap(Crowdloan.State.STATE_CLASS_COMPARATOR)
    }

    private suspend fun crowdloanListFlow(
        chain: Chain,
        contributor: AccountId?,
    ): Flow<List<Crowdloan>> {
        val chainId = chain.id

        val parachainMetadatas = runCatching {
            crowdloanRepository.getParachainMetadata(chain)
        }.getOrDefault(emptyMap())

        return chainStateRepository.currentBlockNumberFlow(chain.id).map { currentBlockNumber ->
            val fundInfos = crowdloanRepository.allFundInfos(chainId)

            val directContributions = contributor?.let { it -> contributionsRepository.getDirectContributions(chain, chain.utilityAsset, it, fundInfos) }
                ?.associateBy { it.paraId } ?: emptyMap()

            val winnerInfo = crowdloanRepository.getWinnerInfo(chainId, fundInfos)

            val expectedBlockTime = chainStateRepository.expectedBlockTimeInMillis(chainId)
            val leasePeriodToBlocksConverter = crowdloanRepository.leasePeriodToBlocksConverter(chainId)

            fundInfos.values
                .map { fundInfo ->
                    val paraId = fundInfo.paraId

                    mapFundInfoToCrowdloan(
                        fundInfo = fundInfo,
                        parachainMetadata = parachainMetadatas[paraId],
                        parachainId = paraId,
                        currentBlockNumber = currentBlockNumber,
                        expectedBlockTimeInMillis = expectedBlockTime,
                        leasePeriodToBlocksConverter = leasePeriodToBlocksConverter,
                        contribution = directContributions[paraId],
                        hasWonAuction = winnerInfo.getValue(paraId)
                    )
                }
                .sortedWith(
                    compareByDescending<Crowdloan> { it.fundInfo.raised }
                        .thenBy { it.fundInfo.end }
                )
        }
    }
}
