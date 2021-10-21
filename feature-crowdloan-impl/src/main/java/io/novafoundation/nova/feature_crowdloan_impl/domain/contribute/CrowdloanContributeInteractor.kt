package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_api.data.repository.hasWonAuction
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.data.network.blockhain.extrinsic.contribute
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.state.chainAndAsset
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import java.math.BigDecimal

typealias AdditionalOnChainSubmission = suspend ExtrinsicBuilder.() -> Unit

class CrowdloanContributeInteractor(
    private val extrinsicService: ExtrinsicService,
    private val accountRepository: AccountRepository,
    private val chainStateRepository: ChainStateRepository,
    private val crowdloanSharedState: CrowdloanSharedState,
    private val crowdloanRepository: CrowdloanRepository
) {

    fun crowdloanStateFlow(
        parachainId: ParaId,
        parachainMetadata: ParachainMetadata? = null
    ): Flow<Crowdloan> = crowdloanSharedState.assetWithChain.flatMapLatest { (chain, _) ->
        val selectedMetaAccount = accountRepository.getSelectedMetaAccount()
        val accountId = selectedMetaAccount.accountIdIn(chain)!! // TODO optional for ethereum chains

        val expectedBlockTime = chainStateRepository.expectedBlockTimeInMillis(chain.id)
        val blocksPerLeasePeriod = crowdloanRepository.blocksPerLeasePeriod(chain.id)

        combine(
            crowdloanRepository.fundInfoFlow(chain.id, parachainId),
            chainStateRepository.currentBlockNumberFlow(chain.id)
        ) { fundInfo, blockNumber ->
            val contribution = crowdloanRepository.getContribution(chain.id, accountId, parachainId, fundInfo.trieIndex)
            val hasWonAuction = crowdloanRepository.hasWonAuction(chain.id, fundInfo)

            mapFundInfoToCrowdloan(
                fundInfo = fundInfo,
                parachainMetadata = parachainMetadata,
                parachainId = parachainId,
                currentBlockNumber = blockNumber,
                expectedBlockTimeInMillis = expectedBlockTime,
                blocksPerLeasePeriod = blocksPerLeasePeriod,
                contribution = contribution,
                hasWonAuction = hasWonAuction
            )
        }
    }

    suspend fun estimateFee(
        parachainId: ParaId,
        contribution: BigDecimal,
        additional: AdditionalOnChainSubmission?
    ) = withContext(Dispatchers.Default) {
        val (chain, chainAsset) = crowdloanSharedState.chainAndAsset()

        val contributionInPlanks = chainAsset.planksFromAmount(contribution)

        extrinsicService.estimateFee(chain) {
            contribute(parachainId, contributionInPlanks)

            additional?.invoke(this)
        }
    }

    suspend fun contribute(
        parachainId: ParaId,
        contribution: BigDecimal,
        additional: AdditionalOnChainSubmission?
    ) = withContext(Dispatchers.Default) {
        val (chain, chainAsset) = crowdloanSharedState.chainAndAsset()
        val selectedMetaAccount = accountRepository.getSelectedMetaAccount()

        val accountId = selectedMetaAccount.accountIdIn(chain)!!
        val contributionInPlanks = chainAsset.planksFromAmount(contribution)

        extrinsicService.submitExtrinsic(chain, accountId) {
            contribute(parachainId, contributionInPlanks)

            additional?.invoke(this)
        }.getOrThrow()
    }
}
