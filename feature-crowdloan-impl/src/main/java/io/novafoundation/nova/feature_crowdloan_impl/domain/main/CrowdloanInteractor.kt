package io.novafoundation.nova.feature_crowdloan_impl.domain.main

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.getContributions
import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.ExternalContributionSource
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.mapFundInfoToCrowdloan
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

class Crowdloans(
    val contributionsCount: Int,
    val crowdloanList: GroupedCrowdloans,
)

typealias GroupedCrowdloans = GroupedList<KClass<out Crowdloan.State>, Crowdloan>

class CrowdloanInteractor(
    private val accountRepository: AccountRepository,
    private val crowdloanRepository: CrowdloanRepository,
    private val chainStateRepository: ChainStateRepository,
    private val externalContributionsSource: ExternalContributionSource,
) {

    fun crowdloansFlow(chain: Chain): Flow<Crowdloans> {
        return flow {
            val chainId = chain.id

            if (crowdloanRepository.isCrowdloansAvailable(chainId).not()) {
                val value = Crowdloans(
                    contributionsCount = 0,
                    crowdloanList = emptyMap()
                )

                emit(value)

                return@flow
            }

            val parachainMetadatas = runCatching {
                crowdloanRepository.getParachainMetadata(chain)
            }.getOrDefault(emptyMap())

            val metaAccount = accountRepository.getSelectedMetaAccount()

            val accountId = metaAccount.accountIdIn(chain)!! // TODO ethereum

            val expectedBlockTime = chainStateRepository.expectedBlockTimeInMillis(chainId)
            val blocksPerLeasePeriod = crowdloanRepository.blocksPerLeasePeriod(chainId)

            val externalContributions = externalContributionsSource.getContributions(chain, accountId)

            val withBlockUpdates = chainStateRepository.currentBlockNumberFlow(chainId).map { currentBlockNumber ->
                val fundInfos = crowdloanRepository.allFundInfos(chainId)

                val directContributions = crowdloanRepository.getContributions(chainId, accountId, fundInfos)
                val directCollectionsCount = directContributions.count { (_, contribution) -> contribution != null }

                val winnerInfo = crowdloanRepository.getWinnerInfo(chainId, fundInfos)

                val groupedCrowdloans = fundInfos.values
                    .map { fundInfo ->
                        val paraId = fundInfo.paraId

                        mapFundInfoToCrowdloan(
                            fundInfo = fundInfo,
                            parachainMetadata = parachainMetadatas[paraId],
                            parachainId = paraId,
                            currentBlockNumber = currentBlockNumber,
                            expectedBlockTimeInMillis = expectedBlockTime,
                            blocksPerLeasePeriod = blocksPerLeasePeriod,
                            contribution = directContributions[paraId],
                            hasWonAuction = winnerInfo.getValue(paraId)
                        )
                    }
                    .sortedWith(
                        compareByDescending<Crowdloan> { it.fundInfo.raised }
                            .thenBy { it.fundInfo.end }
                    )
                    .groupBy { it.state::class }
                    .toSortedMap(Crowdloan.State.STATE_CLASS_COMPARATOR)

                Crowdloans(
                    contributionsCount = directCollectionsCount + externalContributions.size,
                    crowdloanList = groupedCrowdloans
                )
            }

            emitAll(withBlockUpdates)
        }
    }
}
