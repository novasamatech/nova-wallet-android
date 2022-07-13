package io.novafoundation.nova.feature_crowdloan_impl.domain.contributions

import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.getContributions
import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.ExternalContributionSource
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.leasePeriodInMillis
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlin.time.ExperimentalTime

class ContributionsInteractor(
    private val externalContributionSource: ExternalContributionSource,
    private val crowdloanRepository: CrowdloanRepository,
    private val accountRepository: AccountRepository,
    private val selectedAssetState: SingleAssetSharedState,
    private val chainStateRepository: ChainStateRepository,
) {

    @OptIn(ExperimentalTime::class)
    suspend fun getUserContributions(): List<Contribution> {
        val chain = selectedAssetState.chain()
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val accountId = metaAccount.accountIdIn(chain)!!

        if (crowdloanRepository.isCrowdloansAvailable(chain.id).not()) {
            return emptyList()
        }

        val parachainMetadatas = runCatching {
            crowdloanRepository.getParachainMetadata(chain)
        }.getOrDefault(emptyMap())

        val fundInfos = crowdloanRepository.allFundInfos(chain.id)

        val expectedBlockTime = chainStateRepository.expectedBlockTimeInMillis(chain.id)
        val blocksPerLeasePeriod = crowdloanRepository.blocksPerLeasePeriod(chain.id)
        val currentBlockNumber = chainStateRepository.currentBlock(chain.id)

        fun FundInfo.returnDuration(): TimerValue {
            val millis = leasePeriodInMillis(
                blocksPerLeasePeriod = blocksPerLeasePeriod,
                currentBlockNumber = currentBlockNumber,
                endingLeasePeriod = lastSlot,
                expectedBlockTimeInMillis = expectedBlockTime,
            )

            return TimerValue(millis, millisCalculatedAt = System.currentTimeMillis())
        }

        val directContributions = crowdloanRepository.getContributions(chain.id, accountId, fundInfos)
            .mapNotNull { (paraId, directContribution) ->
                directContribution?.let {
                    val fundInfo = fundInfos.getValue(paraId)

                    Contribution(
                        amount = it.amount,
                        paraId = paraId,
                        fundInfo = fundInfo,
                        sourceName = null,
                        parachainMetadata = parachainMetadatas[paraId],
                        returnsIn = fundInfo.returnDuration()
                    )
                }
            }

        val externalContributions = externalContributionSource.getContributions(
            chain = chain,
            accountId = accountId
        )
            .map {
                val fundInfo = fundInfos.getValue(it.paraId)

                Contribution(
                    amount = it.amount,
                    parachainMetadata = parachainMetadatas[it.paraId],
                    sourceName = it.sourceName,
                    fundInfo = fundInfo,
                    paraId = it.paraId,
                    returnsIn = fundInfo.returnDuration()
                )
            }

        return directContributions + externalContributions
    }
}
